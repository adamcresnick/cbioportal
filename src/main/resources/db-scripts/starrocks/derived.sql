-- Copyright (c) 2016 - 2026 Memorial Sloan Kettering Cancer Center.
-- SPDX-License-Identifier: AGPL-3.0-or-later
-- Derived from the cBioPortal ClickHouse derived-table contract.
--
-- cBioPortal v7.0.5 derived read contracts for StarRocks 3.5.19.
-- Derived rows are loaded by the deterministic fixture now and rebuilt by deployment jobs later.

DROP TABLE IF EXISTS sample_to_gene_panel_derived;
DROP TABLE IF EXISTS gene_panel_to_gene_derived;
DROP TABLE IF EXISTS sample_derived;
DROP TABLE IF EXISTS genomic_event_derived;
DROP TABLE IF EXISTS clinical_data_derived;
DROP TABLE IF EXISTS clinical_event_derived;
DROP TABLE IF EXISTS clinical_event_data_derived;
DROP TABLE IF EXISTS genetic_alteration_derived;
DROP TABLE IF EXISTS generic_assay_data_derived;
DROP TABLE IF EXISTS mutation_derived;
DROP TABLE IF EXISTS generic_assay_profile_entity_derived;
DROP TABLE IF EXISTS generic_assay_meta_derived;

CREATE TABLE sample_to_gene_panel_derived (
    `sample_unique_id` VARCHAR(255) NOT NULL,
    `alteration_type` VARCHAR(255) NOT NULL,
    `gene_panel_id` VARCHAR(255) NOT NULL,
    `cancer_study_identifier` VARCHAR(255) NOT NULL,
    `genetic_profile_id` VARCHAR(255) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`sample_unique_id`)
DISTRIBUTED BY HASH(`sample_unique_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE gene_panel_to_gene_derived (
    `gene_panel_id` VARCHAR(255) NOT NULL,
    `gene` VARCHAR(2048) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`gene_panel_id`)
DISTRIBUTED BY HASH(`gene_panel_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE sample_derived (
    `sample_unique_id`            VARCHAR(255) NOT NULL,
    `sample_unique_id_base64`     VARCHAR(2048) NOT NULL,
    `sample_stable_id`            VARCHAR(2048) NOT NULL,
    `patient_unique_id`           VARCHAR(2048) NOT NULL,
    `patient_unique_id_base64`    VARCHAR(2048) NOT NULL,
    `patient_stable_id`           VARCHAR(2048) NOT NULL,
    `cancer_study_identifier`     VARCHAR(255) NOT NULL,
    `internal_id`                 INT NOT NULL,
    -- fields below are needed for the SUMMARY projection
    `patient_internal_id`         INT NOT NULL,
    `sample_type`                 VARCHAR(2048) NOT NULL,
    -- fields below are needed for the DETAILED projection
    `sequenced`                   INT NOT NULL,
    `copy_number_segment_present` INT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`sample_unique_id`)
DISTRIBUTED BY HASH(`sample_unique_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE genomic_event_derived (
    `sample_unique_id`          VARCHAR(255) NOT NULL,
    `hugo_gene_symbol`          VARCHAR(2048) NOT NULL,
    `entrez_gene_id`            INT NOT NULL,
    `gene_panel_stable_id`      VARCHAR(255) NOT NULL,
    `cancer_study_identifier`   VARCHAR(255) NOT NULL,
    `genetic_profile_stable_id` VARCHAR(255) NOT NULL,
    `variant_type`              VARCHAR(255) NOT NULL,
    `mutation_variant`          VARCHAR(2048) NOT NULL,
    `mutation_type`             VARCHAR(255) NOT NULL,
    `mutation_status`           VARCHAR(255) NOT NULL,
    `driver_filter`             VARCHAR(255) NOT NULL,
    `driver_filter_annotation`  VARCHAR(2048) NOT NULL,
    `driver_tiers_filter`       VARCHAR(255) NOT NULL,
    `driver_tiers_filter_annotation` VARCHAR(2048) NOT NULL,
    `cna_alteration`            TINYINT NULL,
    `cna_cytoband`              VARCHAR(2048) NOT NULL,
    `sv_event_info`             VARCHAR(65533) NOT NULL,
    `patient_unique_id`         VARCHAR(2048) NOT NULL,
    `off_panel`                 BOOLEAN NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`sample_unique_id`)
DISTRIBUTED BY HASH(`sample_unique_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE clinical_data_derived (
    `internal_id` INT NOT NULL,
    `sample_unique_id` VARCHAR(2048) NOT NULL,
    `patient_unique_id` VARCHAR(2048) NOT NULL,
    `attribute_name` VARCHAR(255) NOT NULL,
    `attribute_value` VARCHAR(65533) NOT NULL,
    `cancer_study_identifier` VARCHAR(255) NOT NULL,
    `type` VARCHAR(255) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE clinical_event_derived (
    `clinical_event_id` BIGINT NOT NULL,
    `patient_id` BIGINT NULL,
    `patient_stable_id` VARCHAR(2048) NOT NULL,
    `start_date` BIGINT NULL,
    `stop_date` BIGINT NULL,
    `event_type` VARCHAR(255) NOT NULL,
    `cancer_study_identifier` VARCHAR(255) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`clinical_event_id`)
DISTRIBUTED BY HASH(`clinical_event_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE clinical_event_data_derived (
    `patient_unique_id` VARCHAR(255) NOT NULL,
    `key` VARCHAR(2048) NOT NULL,
    `value` VARCHAR(65533) NOT NULL,
    `start_date` INT NOT NULL,
    `stop_date` INT NOT NULL,
    `event_type` VARCHAR(255) NOT NULL,
    `cancer_study_identifier` VARCHAR(255) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`patient_unique_id`)
DISTRIBUTED BY HASH(`patient_unique_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE genetic_alteration_derived (
    `sample_unique_id` VARCHAR(255) NOT NULL,
    `cancer_study_identifier` VARCHAR(255) NOT NULL,
    `hugo_gene_symbol` VARCHAR(2048) NOT NULL,
    `profile_type` VARCHAR(255) NOT NULL,
    `alteration_value` VARCHAR(65533) NULL
    
) ENGINE=OLAP
DUPLICATE KEY(`sample_unique_id`)
DISTRIBUTED BY HASH(`sample_unique_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE generic_assay_data_derived (
    `sample_unique_id` VARCHAR(255) NOT NULL,
    `patient_unique_id` VARCHAR(2048) NOT NULL,
    `genetic_entity_id` VARCHAR(2048) NOT NULL,
    `value` VARCHAR(65533) NOT NULL,
    `generic_assay_type` VARCHAR(2048) NOT NULL,
    `profile_stable_id` VARCHAR(2048) NOT NULL,
    `entity_stable_id` VARCHAR(2048) NOT NULL,
    `datatype` VARCHAR(2048) NOT NULL,
    `patient_level` INT NULL,
    `profile_type` VARCHAR(2048) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`sample_unique_id`)
DISTRIBUTED BY HASH(`sample_unique_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE mutation_derived (
    `molecularProfileId` VARCHAR(255) NOT NULL COMMENT 'Stable ID of the genetic profile',
    `sampleId` VARCHAR(2048) NOT NULL COMMENT 'Stable ID of the sample',
    `sampleInternalId` BIGINT NOT NULL,
    `patientId` VARCHAR(2048) NOT NULL COMMENT 'Stable ID of the patient',
    `entrezGeneId` BIGINT NOT NULL COMMENT 'Entrez Gene ID from mutation table (NOT NULL)',
    `studyId` VARCHAR(2048) NOT NULL COMMENT 'Cancer study identifier',
    `center` VARCHAR(2048) NULL COMMENT 'Sequencing center',
    `mutationStatus` VARCHAR(2048) NULL COMMENT 'Mutation status (e.g., Somatic, Germline)',
    `validationStatus` VARCHAR(2048) NULL COMMENT 'Validation status',
    `tumorAltCount` BIGINT NULL COMMENT 'Tumor alternate allele count',
    `tumorRefCount` BIGINT NULL COMMENT 'Tumor reference allele count',
    `normalAltCount` BIGINT NULL COMMENT 'Normal alternate allele count',
    `normalRefCount` BIGINT NULL COMMENT 'Normal reference allele count',
    `aminoAcidChange` VARCHAR(2048) NULL COMMENT 'Amino acid change',
    `chr` VARCHAR(2048) NULL COMMENT 'Chromosome',
    `startPosition` BIGINT NULL COMMENT 'Start position',
    `endPosition` BIGINT NULL COMMENT 'End position',
    `referenceAllele` VARCHAR(2048) NULL COMMENT 'Reference allele',
    `tumorSeqAllele` VARCHAR(2048) NULL COMMENT 'Tumor sequence allele',
    `proteinChange` VARCHAR(2048) NULL COMMENT 'Protein change',
    `mutationType` VARCHAR(2048) NULL COMMENT 'Type of mutation',
    `ncbiBuild` VARCHAR(2048) NULL COMMENT 'NCBI build version',
    `variantType` VARCHAR(2048) NULL COMMENT 'Variant type',
    `refseqMrnaId` VARCHAR(2048) NULL COMMENT 'RefSeq mRNA ID',
    `proteinPosStart` BIGINT NULL COMMENT 'Protein position start',
    `proteinPosEnd` BIGINT NULL COMMENT 'Protein position end',
    `keyword` VARCHAR(2048) NULL COMMENT 'Keyword',
    `annotationJSON` VARCHAR(65533) NULL COMMENT 'Annotation JSON',
    `driverFilter` VARCHAR(2048) NULL COMMENT 'Driver filter',
    `driverFilterAnnotation` VARCHAR(65533) NULL COMMENT 'Driver filter annotation',
    `driverTiersFilter` VARCHAR(2048) NULL COMMENT 'Driver tiers filter',
    `driverTiersFilterAnnotation` VARCHAR(65533) NULL COMMENT 'Driver tiers filter annotation',
    `GENE.entrezGeneId` BIGINT NULL COMMENT 'Gene entrez ID',
    `GENE.hugoGeneSymbol` VARCHAR(2048) NULL COMMENT 'HUGO gene symbol',
    `GENE.type` VARCHAR(2048) NULL COMMENT 'Gene type',
    `alleleSpecificCopyNumber.ascnIntegerCopyNumber` BIGINT NULL COMMENT 'ASCN integer copy number',
    `alleleSpecificCopyNumber.ascnMethod` VARCHAR(2048) NULL COMMENT 'ASCN method',
    `alleleSpecificCopyNumber.ccfExpectedCopiesUpper` DOUBLE NULL COMMENT 'CCF expected copies upper bound',
    `alleleSpecificCopyNumber.ccfExpectedCopies` DOUBLE NULL COMMENT 'CCF expected copies',
    `alleleSpecificCopyNumber.clonal` VARCHAR(2048) NULL COMMENT 'Clonality annotation',
    `alleleSpecificCopyNumber.minorCopyNumber` BIGINT NULL COMMENT 'Minor copy number',
    `alleleSpecificCopyNumber.expectedAltCopies` BIGINT NULL COMMENT 'Expected alternate copies',
    `alleleSpecificCopyNumber.totalCopyNumber` BIGINT NULL COMMENT 'Total copy number'

) ENGINE=OLAP
DUPLICATE KEY(`molecularProfileId`)
DISTRIBUTED BY HASH(`molecularProfileId`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE generic_assay_profile_entity_derived (
    `profile_stable_id` VARCHAR(255) NOT NULL,
    `entity_stable_id`  VARCHAR(2048) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`profile_stable_id`)
DISTRIBUTED BY HASH(`profile_stable_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE generic_assay_meta_derived (
    `entity_stable_id` VARCHAR(255) NOT NULL,
    `entity_type` VARCHAR(255) NOT NULL,
    `properties` MAP<VARCHAR(255), VARCHAR(2048)> NULL

) ENGINE=OLAP
DUPLICATE KEY(`entity_stable_id`)
DISTRIBUTED BY HASH(`entity_stable_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");
