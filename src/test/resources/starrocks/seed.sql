-- Deterministic cBioPortal fixture for StarRocks schema and repository integration tests.
-- Run schema.sql and derived.sql first. Re-run the full bootstrap to reset this fixture.

INSERT INTO schema_migrations VALUES
  ('1', 'Initial cBioPortal StarRocks read contract', '2026-07-11 00:00:00');

INSERT INTO info VALUES
  ('2.14.5', 'fixture', '1.0.11', 'fixture');

INSERT INTO reference_genome VALUES
  (1, 'Homo sapiens', 'hg38', 'GRCh38', 3209286105, 'https://example.org/hg38', '2013-12-24 00:00:00');

INSERT INTO type_of_cancer VALUES
  ('tissue', 'Tissue', 'Gray', 'TISSUE', NULL),
  ('sr_a', 'StarRocks Study A', 'Red', 'SRA', 'tissue'),
  ('sr_b', 'StarRocks Study B', 'Blue', 'SRB', 'tissue');

INSERT INTO genetic_entity VALUES
  (1, 'GENE', 'TP53'),
  (2, 'GENE', 'EGFR'),
  (3, 'GENE', 'BRAF'),
  (1001, 'GENERIC_ASSAY', 'GA_NUMERIC'),
  (1002, 'GENERIC_ASSAY', 'GA_CATEGORY');

INSERT INTO gene VALUES
  (7157, 'TP53', 1, 'protein-coding'),
  (1956, 'EGFR', 2, 'protein-coding'),
  (673, 'BRAF', 3, 'protein-coding');

INSERT INTO gene_alias VALUES
  (7157, 'P53'),
  (1956, 'ERBB1'),
  (673, 'BRAF1');

INSERT INTO reference_genome_gene VALUES
  (7157, 1, '17', '17p13.1', 7668402, 7687550),
  (1956, 1, '7', '7p11.2', 55019017, 55211628),
  (673, 1, '7', '7q34', 140719327, 140924929);

INSERT INTO cancer_study VALUES
  (1, 'sr_study_a', 'sr_a', 'StarRocks Study A', 'Deterministic fixture study A', 1, NULL, NULL, 'fixture', 0, '2026-07-11 00:00:00', 1),
  (2, 'sr_study_b', 'sr_b', 'StarRocks Study B', 'Deterministic fixture study B', 1, NULL, NULL, 'fixture', 0, '2026-07-11 00:00:00', 1);

INSERT INTO cancer_study_tags VALUES
  (1, '{"source":"fixture"}'),
  (2, '{"source":"fixture"}');

INSERT INTO patient VALUES
  (101, 'PA1', 1), (102, 'PA2', 1), (103, 'PA3', 1),
  (201, 'PB1', 2), (202, 'PB2', 2), (203, 'PB3', 2);

INSERT INTO sample VALUES
  (1001, 'SA1', 'Primary Solid Tumor', 101),
  (1002, 'SA2', 'Metastatic', 101),
  (1003, 'SA3', 'Primary Solid Tumor', 102),
  (1004, 'SA4', 'Primary Solid Tumor', 103),
  (2001, 'SB1', 'Primary Solid Tumor', 201),
  (2002, 'SB2', 'Metastatic', 201),
  (2003, 'SB3', 'Primary Solid Tumor', 202),
  (2004, 'SB4', 'Primary Solid Tumor', 203);

INSERT INTO sample_list VALUES
  (11, 'sr_study_a_all', 'all_cases_in_study', 1, 'All study A samples', 'All fixture samples'),
  (12, 'sr_study_a_sequenced', 'all_cases_with_mutation_data', 1, 'Sequenced study A samples', 'Mutation-profiled samples'),
  (13, 'sr_study_a_cna', 'all_cases_with_cna_data', 1, 'CNA study A samples', 'CNA-profiled samples'),
  (21, 'sr_study_b_all', 'all_cases_in_study', 2, 'All study B samples', 'All fixture samples'),
  (22, 'sr_study_b_sequenced', 'all_cases_with_mutation_data', 2, 'Sequenced study B samples', 'Mutation-profiled samples'),
  (23, 'sr_study_b_cna', 'all_cases_with_cna_data', 2, 'CNA study B samples', 'CNA-profiled samples');

INSERT INTO sample_list_list VALUES
  (11,1001),(11,1002),(11,1003),(11,1004),
  (12,1001),(12,1002),(12,1003),(12,1004),
  (13,1001),(13,1002),(13,1003),(13,1004),
  (21,2001),(21,2002),(21,2003),(21,2004),
  (22,2001),(22,2002),(22,2003),(22,2004),
  (23,2001),(23,2002),(23,2003),(23,2004);

INSERT INTO gene_panel VALUES
  (1, 'TEST_PANEL', 'TP53 and EGFR fixture panel');

INSERT INTO gene_panel_list VALUES
  (1, 7157), (1, 1956);

INSERT INTO genetic_profile VALUES
  (101, 'sr_study_a_mutations', 1, 'MUTATION_EXTENDED', NULL, 'MAF', 'Mutations', 'Fixture mutations', 1, NULL, '1', 0),
  (102, 'sr_study_a_gistic', 1, 'COPY_NUMBER_ALTERATION', NULL, 'DISCRETE', 'CNA', 'Fixture CNA', 1, NULL, '2', 0),
  (103, 'sr_study_a_mrna', 1, 'MRNA_EXPRESSION', NULL, 'CONTINUOUS', 'mRNA', 'Fixture mRNA', 1, NULL, '3', 0),
  (104, 'sr_study_a_structural_variants', 1, 'STRUCTURAL_VARIANT', NULL, 'SV', 'Structural variants', 'Fixture SV', 1, NULL, '4', 0),
  (105, 'sr_study_a_response', 1, 'GENERIC_ASSAY', 'TREATMENT_RESPONSE', 'CONTINUOUS', 'Response score', 'Numeric generic assay', 1, NULL, '5', 0),
  (106, 'sr_study_a_biomarker', 1, 'GENERIC_ASSAY', 'BIOMARKER', 'CATEGORICAL', 'Biomarker', 'Categorical generic assay', 1, NULL, '6', 0),
  (201, 'sr_study_b_mutations', 2, 'MUTATION_EXTENDED', NULL, 'MAF', 'Mutations', 'Fixture mutations', 1, NULL, '1', 0),
  (202, 'sr_study_b_gistic', 2, 'COPY_NUMBER_ALTERATION', NULL, 'DISCRETE', 'CNA', 'Fixture CNA', 1, NULL, '2', 0),
  (203, 'sr_study_b_mrna', 2, 'MRNA_EXPRESSION', NULL, 'CONTINUOUS', 'mRNA', 'Fixture mRNA', 1, NULL, '3', 0),
  (204, 'sr_study_b_structural_variants', 2, 'STRUCTURAL_VARIANT', NULL, 'SV', 'Structural variants', 'Fixture SV', 1, NULL, '4', 0),
  (205, 'sr_study_b_response', 2, 'GENERIC_ASSAY', 'TREATMENT_RESPONSE', 'CONTINUOUS', 'Response score', 'Numeric generic assay', 1, NULL, '5', 0),
  (206, 'sr_study_b_biomarker', 2, 'GENERIC_ASSAY', 'BIOMARKER', 'CATEGORICAL', 'Biomarker', 'Categorical generic assay', 1, NULL, '6', 0);

INSERT INTO genetic_profile_samples VALUES
  (101, '1001,1002,1003,1004'), (102, '1001,1002,1003,1004'), (103, '1001,1002,1003,1004'),
  (105, '1001,1002,1003,1004'), (106, '1001,1002,1003,1004'),
  (201, '2001,2002,2003,2004'), (202, '2001,2002,2003,2004'), (203, '2001,2002,2003,2004'),
  (205, '2001,2002,2003,2004'), (206, '2001,2002,2003,2004');

INSERT INTO sample_profile VALUES
  (1001,101,1),(1002,101,1),(1003,101,NULL),(1004,101,1),
  (1001,102,1),(1002,102,1),(1003,102,NULL),(1004,102,1),
  (1001,103,1),(1002,103,1),(1003,103,NULL),(1004,103,1),
  (2001,201,1),(2002,201,1),(2003,201,NULL),(2004,201,1),
  (2001,202,1),(2002,202,1),(2003,202,NULL),(2004,202,1),
  (2001,203,1),(2002,203,1),(2003,203,NULL),(2004,203,1);

INSERT INTO clinical_attribute_meta VALUES
  ('AGE','Age','Age at diagnosis','NUMBER',1,'1',1),
  ('OS_MONTHS','Overall Survival','Overall survival in months','NUMBER',1,'2',1),
  ('OS_STATUS','Overall Survival Status','Living or deceased','STRING',1,'3',1),
  ('SUBTYPE','Subtype','Sample subtype','STRING',0,'4',1),
  ('PURITY','Purity','Tumor purity','NUMBER',0,'5',1),
  ('SPECIAL','Special Value','NA, empty, and missing coverage','STRING',0,'6',1),
  ('AGE','Age','Age at diagnosis','NUMBER',1,'1',2),
  ('OS_MONTHS','Overall Survival','Overall survival in months','NUMBER',1,'2',2),
  ('OS_STATUS','Overall Survival Status','Living or deceased','STRING',1,'3',2),
  ('SUBTYPE','Subtype','Sample subtype','STRING',0,'4',2),
  ('PURITY','Purity','Tumor purity','NUMBER',0,'5',2),
  ('SPECIAL','Special Value','NA, empty, and missing coverage','STRING',0,'6',2);

INSERT INTO clinical_patient VALUES
  (101,'AGE','10'),(101,'OS_MONTHS','24.5'),(101,'OS_STATUS','1:DECEASED'),
  (102,'AGE','12'),(102,'OS_MONTHS','36'),(102,'OS_STATUS','0:LIVING'),
  (103,'AGE','8'),(103,'OS_STATUS','0:LIVING'),
  (201,'AGE','11'),(201,'OS_MONTHS','18'),(201,'OS_STATUS','1:DECEASED'),
  (202,'AGE','14'),(202,'OS_MONTHS','48'),(202,'OS_STATUS','0:LIVING'),
  (203,'AGE','9'),(203,'OS_MONTHS','NA'),(203,'OS_STATUS','0:LIVING');

INSERT INTO clinical_sample VALUES
  (1001,'SUBTYPE','A'),(1001,'PURITY','0.80'),(1001,'SPECIAL','NA'),
  (1002,'SUBTYPE','B'),(1002,'PURITY','0.55'),(1002,'SPECIAL',''),
  (1003,'SUBTYPE','A'),(1003,'PURITY','NA'),
  (1004,'SUBTYPE','C'),(1004,'PURITY','0.70'),(1004,'SPECIAL','available'),
  (2001,'SUBTYPE','A'),(2001,'PURITY','0.75'),(2001,'SPECIAL','NA'),
  (2002,'SUBTYPE','B'),(2002,'PURITY','0.60'),(2002,'SPECIAL',''),
  (2003,'SUBTYPE','C'),(2003,'PURITY','0.65'),
  (2004,'SUBTYPE','A'),(2004,'PURITY','0.50'),(2004,'SPECIAL','available');

INSERT INTO clinical_event VALUES
  (10001,101,0,NULL,'SPECIMEN'),(10002,101,50,75,'TREATMENT'),(10003,101,100,NULL,'SPECIMEN'),
  (20001,201,0,NULL,'SPECIMEN'),(20002,201,40,65,'TREATMENT'),(20003,201,90,NULL,'SPECIMEN');

INSERT INTO clinical_event_data VALUES
  (10001,'SAMPLE_ID','SA1'),(10002,'AGENT','DrugA'),(10002,'STATUS','COMPLETED'),(10003,'SAMPLE_ID','SA2'),
  (20001,'SAMPLE_ID','SB1'),(20002,'AGENT','DrugB'),(20002,'STATUS','COMPLETED'),(20003,'SAMPLE_ID','SB2');

INSERT INTO mutation_event VALUES
  (1,7157,'17',7674220,7674220,'C','T','R175H','Missense_Mutation','GRCh38','+','SNP',NULL,NULL,'NM_000546','c.524G>A','P04637',175,175,1,'TP53 R175'),
  (2,7157,'17',7673803,7673803,'C','T','R248Q','Missense_Mutation','GRCh38','+','SNP',NULL,NULL,'NM_000546','c.743G>A','P04637',248,248,1,'TP53 R248'),
  (3,1956,'7',55181378,55181378,'T','G','L858R','Missense_Mutation','GRCh38','+','SNP',NULL,NULL,'NM_005228','c.2573T>G','P00533',858,858,1,'EGFR L858'),
  (4,673,'7',140753336,140753336,'A','T','V600E','Missense_Mutation','GRCh38','+','SNP',NULL,NULL,'NM_004333','c.1799T>A','P15056',600,600,1,'BRAF V600');

INSERT INTO mutation
  (mutation_event_id,genetic_profile_id,sample_id,entrez_gene_id,mutation_status,validation_status,sequence_source,tumor_alt_count,tumor_ref_count,normal_alt_count,normal_ref_count,amino_acid_change,annotation_json)
VALUES
  (1,101,1001,7157,'Somatic','VALID','WXS',40,60,0,100,'R175H','{"driver":true}'),
  (2,101,1002,7157,'Somatic','VALID','WXS',35,65,0,100,'R248Q','{"driver":true}'),
  (3,101,1003,1956,'Somatic','VALID','WXS',30,70,0,100,'L858R','{}'),
  (4,201,2001,673,'Somatic','VALID','WXS',45,55,0,100,'V600E','{"driver":true}'),
  (1,201,2002,7157,'Somatic','VALID','WXS',25,75,0,100,'R175H','{}');

INSERT INTO alteration_driver_annotation VALUES
  (1,101,1001,'Putative_Driver','OncoKB','Tier1','Curated'),
  (2,101,1002,'Putative_Driver','OncoKB','Tier1','Curated'),
  (4,201,2001,'Putative_Driver','OncoKB','Tier1','Curated');

INSERT INTO allele_specific_copy_number VALUES
  (1,101,1001,2,'ABSOLUTE',2.2,2.0,'clonal',1,1,2);

INSERT INTO cna_event VALUES
  (1,7157,2),(2,1956,-2),(3,673,1);

INSERT INTO sample_cna_event VALUES
  (1,1001,102,'{}'),(2,1002,102,'{}'),(3,2001,202,'{}');

INSERT INTO genetic_alteration VALUES
  (102,1,'2,0,-1,NA'),(102,2,'0,-2,0,1'),
  (103,1,'1.2,2.5,0.0,NA'),(103,2,'3.1,2.2,1.4,0.5'),
  (105,1001,'0.1,0.5,0.9,NA'),(106,1002,'LOW,HIGH,LOW,NA'),
  (202,1,'0,1,2,-1'),(202,3,'1,0,-2,2'),
  (203,1,'2.0,1.0,3.0,4.0'),(203,3,'0.2,0.4,0.6,0.8'),
  (205,1001,'0.3,0.6,0.2,0.7'),(206,1002,'HIGH,LOW,HIGH,LOW');

INSERT INTO structural_variant
  (internal_id,genetic_profile_id,sample_id,site1_entrez_gene_id,site1_chromosome,site1_position,site2_entrez_gene_id,site2_chromosome,site2_position,event_info,sv_status,annotation_json)
VALUES
  (1,104,1001,7157,'17',7674220,1956,'7',55181378,'TP53-EGFR','SOMATIC','{}'),
  (2,204,2001,673,'7',140753336,7157,'17',7674220,'BRAF-TP53','SOMATIC','{}');

INSERT INTO generic_entity_properties VALUES
  (1,1001,'UNIT','score'),(2,1001,'DATATYPE','NUMBER'),
  (3,1002,'VALUES','LOW,HIGH'),(4,1002,'DATATYPE','CATEGORICAL');

INSERT INTO copy_number_seg VALUES
  (1,1,1001,'17',1,1000000,100,0.25),(2,2,2001,'7',1,1000000,80,-0.20);

INSERT INTO resource_definition VALUES
  ('fixture-study','Fixture Study Resource','Study fixture link','STUDY',1,1,1,NULL),
  ('fixture-patient','Fixture Patient Resource','Patient fixture link','PATIENT',0,2,1,NULL),
  ('fixture-sample','Fixture Sample Resource','Sample fixture link','SAMPLE',0,3,1,NULL);

INSERT INTO resource_study VALUES (1,'fixture-study','https://example.org/study');
INSERT INTO resource_patient VALUES (101,'fixture-patient','https://example.org/patient/PA1');
INSERT INTO resource_sample VALUES (1001,'fixture-sample','https://example.org/sample/SA1');

INSERT INTO sample_derived VALUES
  ('sr_study_a_SA1','c3Jfc3R1ZHlfYV9TQTE=','SA1','sr_study_a_PA1','c3Jfc3R1ZHlfYV9QQTE=','PA1','sr_study_a',1001,101,'Primary Solid Tumor',1,1),
  ('sr_study_a_SA2','c3Jfc3R1ZHlfYV9TQTI=','SA2','sr_study_a_PA1','c3Jfc3R1ZHlfYV9QQTE=','PA1','sr_study_a',1002,101,'Metastatic',1,0),
  ('sr_study_a_SA3','c3Jfc3R1ZHlfYV9TQTM=','SA3','sr_study_a_PA2','c3Jfc3R1ZHlfYV9QQTI=','PA2','sr_study_a',1003,102,'Primary Solid Tumor',1,0),
  ('sr_study_a_SA4','c3Jfc3R1ZHlfYV9TTQQ=','SA4','sr_study_a_PA3','c3Jfc3R1ZHlfYV9QQTM=','PA3','sr_study_a',1004,103,'Primary Solid Tumor',1,0),
  ('sr_study_b_SB1','c3Jfc3R1ZHlfYl9TQjE=','SB1','sr_study_b_PB1','c3Jfc3R1ZHlfYl9QQjE=','PB1','sr_study_b',2001,201,'Primary Solid Tumor',1,1),
  ('sr_study_b_SB2','c3Jfc3R1ZHlfYl9TQjI=','SB2','sr_study_b_PB1','c3Jfc3R1ZHlfYl9QQjE=','PB1','sr_study_b',2002,201,'Metastatic',1,0),
  ('sr_study_b_SB3','c3Jfc3R1ZHlfYl9TQjM=','SB3','sr_study_b_PB2','c3Jfc3R1ZHlfYl9QQjI=','PB2','sr_study_b',2003,202,'Primary Solid Tumor',1,0),
  ('sr_study_b_SB4','c3Jfc3R1ZHlfYl9TQjQ=','SB4','sr_study_b_PB3','c3Jfc3R1ZHlfYl9QQjM=','PB3','sr_study_b',2004,203,'Primary Solid Tumor',1,0);

INSERT INTO sample_to_gene_panel_derived VALUES
  ('sr_study_a_SA1','MUTATION_EXTENDED','TEST_PANEL','sr_study_a','sr_study_a_mutations'),
  ('sr_study_a_SA2','MUTATION_EXTENDED','TEST_PANEL','sr_study_a','sr_study_a_mutations'),
  ('sr_study_a_SA3','MUTATION_EXTENDED','WES','sr_study_a','sr_study_a_mutations'),
  ('sr_study_a_SA4','MUTATION_EXTENDED','TEST_PANEL','sr_study_a','sr_study_a_mutations'),
  ('sr_study_b_SB1','MUTATION_EXTENDED','TEST_PANEL','sr_study_b','sr_study_b_mutations'),
  ('sr_study_b_SB2','MUTATION_EXTENDED','TEST_PANEL','sr_study_b','sr_study_b_mutations'),
  ('sr_study_b_SB3','MUTATION_EXTENDED','WES','sr_study_b','sr_study_b_mutations'),
  ('sr_study_b_SB4','MUTATION_EXTENDED','TEST_PANEL','sr_study_b','sr_study_b_mutations');

INSERT INTO gene_panel_to_gene_derived VALUES
  ('TEST_PANEL','TP53'),('TEST_PANEL','EGFR'),('WES','TP53'),('WES','EGFR'),('WES','BRAF');

INSERT INTO clinical_data_derived VALUES
  (101,'','sr_study_a_PA1','AGE','10','sr_study_a','patient'),
  (101,'','sr_study_a_PA1','OS_MONTHS','24.5','sr_study_a','patient'),
  (101,'','sr_study_a_PA1','OS_STATUS','1:DECEASED','sr_study_a','patient'),
  (1001,'sr_study_a_SA1','sr_study_a_PA1','SUBTYPE','A','sr_study_a','sample'),
  (1001,'sr_study_a_SA1','sr_study_a_PA1','PURITY','0.80','sr_study_a','sample'),
  (1002,'sr_study_a_SA2','sr_study_a_PA1','SPECIAL','','sr_study_a','sample'),
  (201,'','sr_study_b_PB1','AGE','11','sr_study_b','patient'),
  (201,'','sr_study_b_PB1','OS_MONTHS','18','sr_study_b','patient'),
  (2001,'sr_study_b_SB1','sr_study_b_PB1','SUBTYPE','A','sr_study_b','sample'),
  (2003,'sr_study_b_SB3','sr_study_b_PB2','PURITY','0.65','sr_study_b','sample');

INSERT INTO clinical_event_derived VALUES
  (10001,101,'PA1',0,NULL,'SPECIMEN','sr_study_a'),
  (10002,101,'PA1',50,75,'TREATMENT','sr_study_a'),
  (10003,101,'PA1',100,NULL,'SPECIMEN','sr_study_a'),
  (20001,201,'PB1',0,NULL,'SPECIMEN','sr_study_b'),
  (20002,201,'PB1',40,65,'TREATMENT','sr_study_b'),
  (20003,201,'PB1',90,NULL,'SPECIMEN','sr_study_b');

INSERT INTO clinical_event_data_derived
  (`patient_unique_id`,`key`,`value`,`start_date`,`stop_date`,`event_type`,`cancer_study_identifier`)
VALUES
  ('sr_study_a_PA1','SAMPLE_ID','SA1',0,0,'SPECIMEN','sr_study_a'),
  ('sr_study_a_PA1','AGENT','DrugA',50,75,'TREATMENT','sr_study_a'),
  ('sr_study_a_PA1','SAMPLE_ID','SA2',100,100,'SPECIMEN','sr_study_a'),
  ('sr_study_b_PB1','SAMPLE_ID','SB1',0,0,'SPECIMEN','sr_study_b'),
  ('sr_study_b_PB1','AGENT','DrugB',40,65,'TREATMENT','sr_study_b'),
  ('sr_study_b_PB1','SAMPLE_ID','SB2',90,90,'SPECIMEN','sr_study_b');

INSERT INTO genomic_event_derived VALUES
  ('sr_study_a_SA1','TP53',7157,'TEST_PANEL','sr_study_a','sr_study_a_mutations','mutation','R175H','Missense_Mutation','Somatic','Putative_Driver','OncoKB','Tier1','Curated',NULL,'','','sr_study_a_PA1',0),
  ('sr_study_a_SA2','TP53',7157,'TEST_PANEL','sr_study_a','sr_study_a_mutations','mutation','R248Q','Missense_Mutation','Somatic','Putative_Driver','OncoKB','Tier1','Curated',NULL,'','','sr_study_a_PA1',0),
  ('sr_study_a_SA3','EGFR',1956,'WES','sr_study_a','sr_study_a_mutations','mutation','L858R','Missense_Mutation','Somatic','','','','',NULL,'','','sr_study_a_PA2',0),
  ('sr_study_b_SB1','BRAF',673,'TEST_PANEL','sr_study_b','sr_study_b_mutations','mutation','V600E','Missense_Mutation','Somatic','Putative_Driver','OncoKB','Tier1','Curated',NULL,'','','sr_study_b_PB1',1),
  ('sr_study_b_SB2','TP53',7157,'TEST_PANEL','sr_study_b','sr_study_b_mutations','mutation','R175H','Missense_Mutation','Somatic','','','','',NULL,'','','sr_study_b_PB1',0),
  ('sr_study_a_SA1','TP53',7157,'TEST_PANEL','sr_study_a','sr_study_a_gistic','cna','NA','NA','NA','','','','',2,'17p13.1','','sr_study_a_PA1',0),
  ('sr_study_a_SA1','TP53',7157,'TEST_PANEL','sr_study_a','sr_study_a_structural_variants','structural_variant','NA','NA','NA','','','','',NULL,'','TP53-EGFR','sr_study_a_PA1',0),
  ('sr_study_b_SB1','BRAF',673,'TEST_PANEL','sr_study_b','sr_study_b_structural_variants','structural_variant','NA','NA','NA','','','','',NULL,'','BRAF-TP53','sr_study_b_PB1',0);

INSERT INTO genetic_alteration_derived VALUES
  ('sr_study_a_SA1','sr_study_a','TP53','gistic','2'),
  ('sr_study_a_SA2','sr_study_a','TP53','gistic','0'),
  ('sr_study_a_SA1','sr_study_a','TP53','mrna','1.2'),
  ('sr_study_b_SB1','sr_study_b','BRAF','gistic','1'),
  ('sr_study_b_SB1','sr_study_b','TP53','mrna','2.0');

INSERT INTO generic_assay_data_derived VALUES
  ('sr_study_a_SA1','sr_study_a_PA1','1001','0.1','TREATMENT_RESPONSE','sr_study_a_response','GA_NUMERIC','CONTINUOUS',0,'response'),
  ('sr_study_a_SA1','sr_study_a_PA1','1002','LOW','BIOMARKER','sr_study_a_biomarker','GA_CATEGORY','CATEGORICAL',0,'biomarker'),
  ('sr_study_b_SB1','sr_study_b_PB1','1001','0.3','TREATMENT_RESPONSE','sr_study_b_response','GA_NUMERIC','CONTINUOUS',0,'response'),
  ('sr_study_b_SB1','sr_study_b_PB1','1002','HIGH','BIOMARKER','sr_study_b_biomarker','GA_CATEGORY','CATEGORICAL',0,'biomarker');

INSERT INTO mutation_derived
  (`molecularProfileId`,`sampleId`,`sampleInternalId`,`patientId`,`entrezGeneId`,`studyId`,`proteinChange`,`mutationType`,`annotationJSON`,`driverFilter`)
VALUES
  ('sr_study_a_mutations','SA1',1001,'PA1',7157,'sr_study_a','R175H','Missense_Mutation','{"driver":true}','Putative_Driver'),
  ('sr_study_a_mutations','SA2',1002,'PA1',7157,'sr_study_a','R248Q','Missense_Mutation','{"driver":true}','Putative_Driver'),
  ('sr_study_a_mutations','SA3',1003,'PA2',1956,'sr_study_a','L858R','Missense_Mutation','{}',NULL),
  ('sr_study_b_mutations','SB1',2001,'PB1',673,'sr_study_b','V600E','Missense_Mutation','{"driver":true}','Putative_Driver'),
  ('sr_study_b_mutations','SB2',2002,'PB1',7157,'sr_study_b','R175H','Missense_Mutation','{}',NULL);

INSERT INTO generic_assay_profile_entity_derived VALUES
  ('sr_study_a_response','GA_NUMERIC'),('sr_study_a_biomarker','GA_CATEGORY'),
  ('sr_study_b_response','GA_NUMERIC'),('sr_study_b_biomarker','GA_CATEGORY');

INSERT INTO generic_assay_meta_derived
  (`entity_stable_id`,`entity_type`,`properties`)
VALUES
  ('GA_NUMERIC','GENERIC_ASSAY',map{'UNIT':'score','DATATYPE':'NUMBER'}),
  ('GA_CATEGORY','GENERIC_ASSAY',map{'VALUES':'LOW,HIGH','DATATYPE':'CATEGORICAL'});
