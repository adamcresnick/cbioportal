package org.cbioportal.infrastructure.repository.starrocks.coexpression;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface StarrocksCoExpressionMapper {

  List<StarrocksCoExpressionAggregateRow> getCoExpressionAggregates(
      @Param("cancerStudyIdentifierA") String cancerStudyIdentifierA,
      @Param("profileTypeA") String profileTypeA,
      @Param("cancerStudyIdentifierB") String cancerStudyIdentifierB,
      @Param("profileTypeB") String profileTypeB,
      @Param("hugoGeneSymbol") String hugoGeneSymbol,
      @Param("sampleUniqueIds") List<String> sampleUniqueIds);
}
