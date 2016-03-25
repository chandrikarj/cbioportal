/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mskcc.cbio.portal.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.mskcc.cbio.portal.model.DBAltCount;

/**
 *
 * @author jiaojiao
 */
public interface MutationMapper {
    
    List<DBAltCount> getMutationsCountsPerStudyWithoutPosition(@Param("gene") String gene);
    List<DBAltCount> getMutationsCountsPerStudy(@Param("gene") String gene, @Param("start") Integer start, @Param("end") Integer end);
    
    List<DBAltCount> getMutationsCountsPerStudyWithIdsWithoutPosition(@Param("gene") String gene, @Param("studyIds") List<String> studyIds);
    List<DBAltCount> getMutationsCountsPerStudyWithIds(@Param("gene") String gene, @Param("start") Integer start, @Param("end") Integer end, @Param("studyIds") List<String> studyIds);
    
    DBAltCount getMutationsCountsWithoutPosition(@Param("gene") String gene);
    DBAltCount getMutationsCounts(@Param("gene") String gene, @Param("start") Integer start, @Param("end") Integer end);
    
}
