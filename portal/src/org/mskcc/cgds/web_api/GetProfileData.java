package org.mskcc.cgds.web_api;

import org.mskcc.cgds.dao.*;
import org.mskcc.cgds.model.*;
import org.mskcc.cgds.servlet.WebService;
import org.mskcc.portal.model.ProfileData;
import org.mskcc.portal.util.WebFileConnect;

import java.util.ArrayList;
import java.io.IOException;

/**
 * Web Service to Get Profile Data.
 *
 * @author Ethan Cerami.
 */
public class GetProfileData {
    public static int ID_ENTREZ_GENE = 1;
    public static int GENE_SYMBOL = 0;
    private String rawContent;
    private String[][] matrix;
    private ProfileData profileData;
    private ArrayList<String> warningList = new ArrayList<String>();

    /**
     * Constructor.
     * @param targetGeneticProfileIdList    Target Genetic Profile List.
     * @param targetGeneList                Target Gene List.
     * @param targetCaseList                Target Case List.
     * @param suppressMondrianHeader        Flag to suppress the mondrian header.
     * @throws DaoException Database Error.
     * @throws IOException IO Error.
     */
    public GetProfileData (ArrayList<String> targetGeneticProfileIdList,
										ArrayList<String> targetGeneList,
										ArrayList<String> targetCaseList,
										Boolean suppressMondrianHeader)
            throws DaoException, IOException {
        execute(targetGeneticProfileIdList, targetGeneList, targetCaseList, suppressMondrianHeader);
    }

    /**
     * Constructor.
     *
     * @param geneticProfile    Genetic Profile Object.
     * @param targetGeneList    Target Gene List.
     * @param caseIds           White-space delimited case IDs.
     * @throws DaoException     Database Error.
     * @throws IOException      IO Error.
     */
    public GetProfileData (GeneticProfile geneticProfile, ArrayList<String> targetGeneList,
            String caseIds) throws DaoException, IOException {
        ArrayList<String> targetGeneticProfileIdList = new ArrayList<String>();
        targetGeneticProfileIdList.add(geneticProfile.getStableId());

        ArrayList<String> targetCaseList = new ArrayList<String>();
        String caseIdParts[] = caseIds.split("\\s");
        for (String caseIdPart: caseIdParts) {
            targetCaseList.add(caseIdPart);
        }
        execute(targetGeneticProfileIdList, targetGeneList, targetCaseList, true);
    }

    /**
     * Gets the Raw Content Generated by the Web API.
     * @return Raw Content.
     */
    public String getRawContent() {
        return rawContent;
    }

    /**
     * Gets the Data Matrix Generated by the Web API.
     * @return Matrix of Strings.
     */
    public String[][] getMatrix() {
        return matrix;
    }

    /**
     * Gets the Profile Data Object Generated by the Web API.
     * @return ProfileData Object.
     */
    public ProfileData getProfileData() {
        return profileData;
    }

    /**
     * Gets warnings (if triggered).
     *
     * @return ArrayList of Warning Strings.
     */
    public ArrayList<String> getWarnings() {
        return this.warningList;
    }

    /**
     * Executes the LookUp.
     */
    private void execute(ArrayList<String> targetGeneticProfileIdList,
            ArrayList<String> targetGeneList, ArrayList<String> targetCaseList,
            Boolean suppressMondrianHeader) throws DaoException, IOException {
        this.rawContent = getProfileData (targetGeneticProfileIdList, targetGeneList,
                targetCaseList, suppressMondrianHeader);
        this.matrix = WebFileConnect.parseMatrix(rawContent);

        //  Create the Profile Data Object
        DaoGeneticProfile daoGeneticProfile = new DaoGeneticProfile();
        if (targetGeneticProfileIdList.size() == 1) {
            String geneticProfileId = targetGeneticProfileIdList.get(0);
            GeneticProfile geneticProfile =
                    daoGeneticProfile.getGeneticProfileByStableId(geneticProfileId);
            profileData = new ProfileData(geneticProfile, matrix);
        }
    }

    /**
     * Gets Profile Data for Specified Target Info.
     * @param targetGeneticProfileIdList    Target Genetic Profile List.
     * @param targetGeneList                Target Gene List.
     * @param targetCaseList                Target Case List.
     * @param suppressMondrianHeader        Flag to suppress the mondrian header.
     * @return Tab Delim Text String.
     * @throws DaoException Database Error.
     */
    private String getProfileData(ArrayList<String> targetGeneticProfileIdList,
										ArrayList<String> targetGeneList, 
										ArrayList<String> targetCaseList,
										Boolean suppressMondrianHeader) throws DaoException {

        StringBuffer buf = new StringBuffer();

        //  Validate that all Genetic Profiles are valid Stable IDs.
        DaoGeneticProfile daoGeneticProfile = new DaoGeneticProfile();
        for (String geneticProfileId:  targetGeneticProfileIdList) {
            GeneticProfile geneticProfile =
                    daoGeneticProfile.getGeneticProfileByStableId(geneticProfileId);
            if (geneticProfile == null) {
                buf.append ("No genetic profile available for "
                        + WebService.GENETIC_PROFILE_ID + ":  "
                        + geneticProfileId + "." + WebApiUtil.NEW_LINE);
                return buf.toString();
            }
        }

        //  Branch based on number of profiles requested.
        //  In the first case, we have 1 profile and 1 or more genes.
        //  In the second case, we have > 1 profiles and only 1 gene.
        if (targetGeneticProfileIdList.size() == 1) {
            String geneticProfileId = targetGeneticProfileIdList.get(0);
            GeneticProfile geneticProfile = daoGeneticProfile.getGeneticProfileByStableId(geneticProfileId);

            //  Get the Gene List
            ArrayList<Gene> geneList = WebApiUtil.getGeneList(targetGeneList,
                    geneticProfile.getGeneticAlterationType(), buf, warningList);
            
            //  Output DATA_TYPE and COLOR_GRADIENT_SETTINGS (Used by Mondrian Cytoscape PlugIn)
            if (!suppressMondrianHeader) {
                buf.append ("# DATA_TYPE\t " + geneticProfile.getProfileName() +"\n");
                buf.append ("# COLOR_GRADIENT_SETTINGS\t "
                            + geneticProfile.getGeneticAlterationType() + "\n");
            }

            //  Ouput Column Headings
            buf.append ("GENE_ID\tCOMMON");
            outputRow(targetCaseList, buf);

            //  Iterate through all validated genes, and extract profile data.
            for (Gene gene: geneList) {                
                ArrayList<String> dataRow = GeneticAlterationUtil.getGeneticAlterationDataRow(gene,
                        targetCaseList, geneticProfile);
                outputGeneRow(dataRow, gene, buf);
            }
        } else {
            //  Ouput Column Headings
            buf.append ("GENETIC_PROFILE_ID\tALTERATION_TYPE\tGENE_ID\tCOMMON");
            outputRow(targetCaseList, buf);
            
            ArrayList<GeneticProfile> profiles = new ArrayList<GeneticProfile>(targetGeneticProfileIdList.size());
            boolean includeRPPAProteinLevel = false;
            for (String gId:  targetGeneticProfileIdList) {
                GeneticProfile profile = daoGeneticProfile.getGeneticProfileByStableId(gId);
                profiles.add(profile);
                if (profile.getGeneticAlterationType() == GeneticAlterationType.PROTEIN_ARRAY_PROTEIN_LEVEL)
                    includeRPPAProteinLevel = true;
            }

            // for rppa protein level, choose the best correlated one
            if (includeRPPAProteinLevel && profiles.size()==2) {
                GeneticProfile gp1, gp2;
                if (profiles.get(0).getGeneticAlterationType() == GeneticAlterationType.PROTEIN_ARRAY_PROTEIN_LEVEL) {
                    gp1 = profiles.get(1);
                    gp2 = profiles.get(0);
                } else {
                    gp1 = profiles.get(0);
                    gp2 = profiles.get(1);
                }
                
                // get data of the other profile
                ArrayList<String> dataRow1 = null;
                ArrayList<Gene> geneList = WebApiUtil.getGeneList(targetGeneList,
                        gp1.getGeneticAlterationType(), buf, warningList);
                
                if (geneList.size() > 0) {
                    Gene gene = geneList.get(0);
                    buf.append (gp1.getStableId() + WebApiUtil.TAB
                            + gp1.getGeneticAlterationType() + WebApiUtil.TAB);   
                    dataRow1 = GeneticAlterationUtil.getGeneticAlterationDataRow(gene,
                            targetCaseList, gp1);
                    outputGeneRow(dataRow1, gene, buf);
                }
                
                // get data of protein array
                geneList = WebApiUtil.getGeneList(targetGeneList,
                        gp2.getGeneticAlterationType(), buf, warningList);
                
                if (geneList.size() > 0) {
                    Gene gene = geneList.get(0);
                    buf.append (gp2.getStableId() + WebApiUtil.TAB
                            + gp2.getGeneticAlterationType() + WebApiUtil.TAB);   
                    ArrayList<String> dataRow = GeneticAlterationUtil.getBestCorrelatedProteinArrayDataRow(
                            (CanonicalGene)gene, targetCaseList, dataRow1);
                    outputGeneRow(dataRow, gene, buf);
                }
            } else {            
                //  Iterate through all genetic profiles
                for (GeneticProfile geneticProfile : profiles) {
                    //  Get the Gene List
                    ArrayList<Gene> geneList = WebApiUtil.getGeneList(targetGeneList,
                            geneticProfile.getGeneticAlterationType(), buf, warningList);

                    if (geneList.size() > 0) {
                        Gene gene = geneList.get(0);
                        buf.append (geneticProfile.getStableId() + WebApiUtil.TAB
                                + geneticProfile.getGeneticAlterationType() + WebApiUtil.TAB);   
                        ArrayList<String> dataRow = GeneticAlterationUtil.getGeneticAlterationDataRow(gene,
                                targetCaseList, geneticProfile);
                        outputGeneRow(dataRow, gene, buf);
                    }
                }
            }
        }
        return buf.toString();
    }

    private static void outputRow(ArrayList<String> dataValues, StringBuffer buf) {
        for (String value:  dataValues) {
            buf.append (WebApiUtil.TAB + value);
        }
        buf.append (WebApiUtil.NEW_LINE);
    }

    private static void outputGeneRow(ArrayList<String> dataRow, Gene gene, StringBuffer buf)
            throws DaoException {
        if (gene instanceof CanonicalGene) {
            CanonicalGene canonicalGene = (CanonicalGene) gene;
            buf.append (canonicalGene.getEntrezGeneId() + WebApiUtil.TAB);
            buf.append (canonicalGene.getHugoGeneSymbolAllCaps());
        } else if (gene instanceof MicroRna) {
            MicroRna microRna = (MicroRna) gene;
            buf.append ("-999999" + WebApiUtil.TAB);
            buf.append (microRna.getMicroRnaId());
        }
        outputRow (dataRow, buf);
    }
}