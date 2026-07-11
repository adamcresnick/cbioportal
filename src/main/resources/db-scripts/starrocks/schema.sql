-- Copyright (c) 2016 - 2026 Memorial Sloan Kettering Cancer Center.
-- SPDX-License-Identifier: AGPL-3.0-or-later
-- Derived from the cBioPortal ClickHouse normalized schema contract.
--
-- cBioPortal v7.0.5 normalized read schema for StarRocks 3.5.19.
-- This script deliberately recreates the contract so bootstrap is deterministic.
-- Select the target database before executing it.

DROP TABLE IF EXISTS allele_specific_copy_number;
DROP TABLE IF EXISTS alteration_driver_annotation;
DROP TABLE IF EXISTS authorities;
DROP TABLE IF EXISTS cancer_study;
DROP TABLE IF EXISTS cancer_study_tags;
DROP TABLE IF EXISTS clinical_attribute_meta;
DROP TABLE IF EXISTS clinical_event;
DROP TABLE IF EXISTS clinical_event_data;
DROP TABLE IF EXISTS clinical_patient;
DROP TABLE IF EXISTS clinical_sample;
DROP TABLE IF EXISTS cna_event;
DROP TABLE IF EXISTS copy_number_seg;
DROP TABLE IF EXISTS copy_number_seg_file;
DROP TABLE IF EXISTS data_access_tokens;
DROP TABLE IF EXISTS gene;
DROP TABLE IF EXISTS gene_alias;
DROP TABLE IF EXISTS gene_panel;
DROP TABLE IF EXISTS gene_panel_list;
DROP TABLE IF EXISTS generic_entity_properties;
DROP TABLE IF EXISTS geneset;
DROP TABLE IF EXISTS geneset_gene;
DROP TABLE IF EXISTS geneset_hierarchy_leaf;
DROP TABLE IF EXISTS geneset_hierarchy_node;
DROP TABLE IF EXISTS genetic_alteration;
DROP TABLE IF EXISTS genetic_entity;
DROP TABLE IF EXISTS genetic_profile;
DROP TABLE IF EXISTS genetic_profile_link;
DROP TABLE IF EXISTS genetic_profile_samples;
DROP TABLE IF EXISTS gistic;
DROP TABLE IF EXISTS gistic_to_gene;
DROP TABLE IF EXISTS info;
DROP TABLE IF EXISTS mut_sig;
DROP TABLE IF EXISTS mutation;
DROP TABLE IF EXISTS mutation_count_by_keyword;
DROP TABLE IF EXISTS mutation_event;
DROP TABLE IF EXISTS patient;
DROP TABLE IF EXISTS reference_genome;
DROP TABLE IF EXISTS reference_genome_gene;
DROP TABLE IF EXISTS resource_definition;
DROP TABLE IF EXISTS resource_patient;
DROP TABLE IF EXISTS resource_sample;
DROP TABLE IF EXISTS resource_study;
DROP TABLE IF EXISTS schema_migrations;
DROP TABLE IF EXISTS sample;
DROP TABLE IF EXISTS sample_cna_event;
DROP TABLE IF EXISTS sample_list;
DROP TABLE IF EXISTS sample_list_list;
DROP TABLE IF EXISTS sample_profile;
DROP TABLE IF EXISTS structural_variant;
DROP TABLE IF EXISTS type_of_cancer;
DROP TABLE IF EXISTS users;

CREATE TABLE allele_specific_copy_number (
    `mutation_event_id` BIGINT NOT NULL,
    `genetic_profile_id` BIGINT NOT NULL,
    `sample_id` BIGINT NOT NULL,
    `ascn_integer_copy_number` BIGINT NULL,
    `ascn_method` VARCHAR(2048) NOT NULL,
    `ccf_expected_copies_upper` DOUBLE NULL,
    `ccf_expected_copies` DOUBLE NULL,
    `clonal` VARCHAR(2048) NULL,
    `minor_copy_number` BIGINT NULL,
    `expected_alt_copies` BIGINT NULL,
    `total_copy_number` BIGINT NULL

) ENGINE=OLAP
DUPLICATE KEY(`mutation_event_id`)
DISTRIBUTED BY HASH(`mutation_event_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE alteration_driver_annotation (
    `alteration_event_id` BIGINT NOT NULL,
    `genetic_profile_id` BIGINT NOT NULL,
    `sample_id` BIGINT NOT NULL,
    `driver_filter` VARCHAR(2048) NULL,
    `driver_filter_annotation` VARCHAR(2048) NULL,
    `driver_tiers_filter` VARCHAR(2048) NULL,
    `driver_tiers_filter_annotation` VARCHAR(2048) NULL

) ENGINE=OLAP
DUPLICATE KEY(`alteration_event_id`)
DISTRIBUTED BY HASH(`alteration_event_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE authorities (
    `email` VARCHAR(255) NOT NULL,
    `authority` VARCHAR(2048) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`email`)
DISTRIBUTED BY HASH(`email`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE cancer_study (
    `cancer_study_id` BIGINT NOT NULL,
    `cancer_study_identifier` VARCHAR(2048) NULL,
    `type_of_cancer_id` VARCHAR(2048) NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `description` VARCHAR(65533) NOT NULL,
    `public` INT NOT NULL,
    `pmid` VARCHAR(2048) NULL,
    `citation` VARCHAR(2048) NULL,
    `groups` VARCHAR(2048) NULL,
    `status` BIGINT NULL,
    `import_date` DATETIME NULL,
    `reference_genome_id` BIGINT NULL

) ENGINE=OLAP
DUPLICATE KEY(`cancer_study_id`)
DISTRIBUTED BY HASH(`cancer_study_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE cancer_study_tags (
    `cancer_study_id` BIGINT NOT NULL,
    `tags` VARCHAR(65533) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`cancer_study_id`)
DISTRIBUTED BY HASH(`cancer_study_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE clinical_attribute_meta (
    `attr_id` VARCHAR(255) NOT NULL,
    `display_name` VARCHAR(2048) NOT NULL,
    `description` VARCHAR(65533) NOT NULL,
    `datatype` VARCHAR(2048) NOT NULL,
    `patient_attribute` INT NOT NULL,
    `priority` VARCHAR(2048) NOT NULL,
    `cancer_study_id` BIGINT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`attr_id`)
DISTRIBUTED BY HASH(`attr_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE clinical_event (
    `clinical_event_id` BIGINT NOT NULL,
    `patient_id` BIGINT NOT NULL,
    `start_date` BIGINT NOT NULL,
    `stop_date` BIGINT NULL,
    `event_type` VARCHAR(2048) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`clinical_event_id`)
DISTRIBUTED BY HASH(`clinical_event_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE clinical_event_data (
    `clinical_event_id` BIGINT NOT NULL,
    `key` VARCHAR(2048) NOT NULL,
    `value` VARCHAR(65533) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`clinical_event_id`)
DISTRIBUTED BY HASH(`clinical_event_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE clinical_patient (
    `internal_id` BIGINT NOT NULL,
    `attr_id` VARCHAR(2048) NOT NULL,
    `attr_value` VARCHAR(65533) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE clinical_sample (
    `internal_id` BIGINT NOT NULL,
    `attr_id` VARCHAR(2048) NOT NULL,
    `attr_value` VARCHAR(65533) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE cna_event (
    `cna_event_id` BIGINT NOT NULL,
    `entrez_gene_id` BIGINT NOT NULL,
    `alteration` INT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`cna_event_id`)
DISTRIBUTED BY HASH(`cna_event_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE copy_number_seg (
    `seg_id` BIGINT NOT NULL,
    `cancer_study_id` BIGINT NOT NULL,
    `sample_id` BIGINT NOT NULL,
    `chr` VARCHAR(2048) NOT NULL,
    `start` BIGINT NOT NULL,
    `end` BIGINT NOT NULL,
    `num_probes` BIGINT NOT NULL,
    `segment_mean` DOUBLE NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`seg_id`)
DISTRIBUTED BY HASH(`seg_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE copy_number_seg_file (
    `seg_file_id` BIGINT NOT NULL,
    `cancer_study_id` BIGINT NOT NULL,
    `reference_genome_id` VARCHAR(2048) NOT NULL,
    `description` VARCHAR(65533) NOT NULL,
    `filename` VARCHAR(2048) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`seg_file_id`)
DISTRIBUTED BY HASH(`seg_file_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE data_access_tokens (
    `token` VARCHAR(255) NOT NULL,
    `username` VARCHAR(2048) NOT NULL,
    `expiration` DATETIME NOT NULL,
    `creation` DATETIME NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`token`)
DISTRIBUTED BY HASH(`token`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE gene (
    `entrez_gene_id` BIGINT NOT NULL,
    `hugo_gene_symbol` VARCHAR(2048) NOT NULL,
    `genetic_entity_id` BIGINT NOT NULL,
    `type` VARCHAR(2048) NULL

) ENGINE=OLAP
DUPLICATE KEY(`entrez_gene_id`)
DISTRIBUTED BY HASH(`entrez_gene_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE gene_alias (
    `entrez_gene_id` BIGINT NOT NULL,
    `gene_alias` VARCHAR(2048) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`entrez_gene_id`)
DISTRIBUTED BY HASH(`entrez_gene_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE gene_panel (
    `internal_id` BIGINT NOT NULL,
    `stable_id` VARCHAR(2048) NOT NULL,
    `description` VARCHAR(65533) NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE gene_panel_list (
    `internal_id` BIGINT NOT NULL,
    `gene_id` BIGINT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE generic_entity_properties (
    `id` BIGINT NOT NULL,
    `genetic_entity_id` BIGINT NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `value` VARCHAR(65533) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`id`)
DISTRIBUTED BY HASH(`id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE geneset (
    `id` BIGINT NOT NULL,
    `genetic_entity_id` BIGINT NOT NULL,
    `external_id` VARCHAR(2048) NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `description` VARCHAR(65533) NOT NULL,
    `ref_link` VARCHAR(2048) NULL

) ENGINE=OLAP
DUPLICATE KEY(`id`)
DISTRIBUTED BY HASH(`id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE geneset_gene (
    `geneset_id` BIGINT NOT NULL,
    `entrez_gene_id` BIGINT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`geneset_id`)
DISTRIBUTED BY HASH(`geneset_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE geneset_hierarchy_leaf (
    `node_id` BIGINT NOT NULL,
    `geneset_id` BIGINT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`node_id`)
DISTRIBUTED BY HASH(`node_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE geneset_hierarchy_node (
    `node_id` BIGINT NOT NULL,
    `node_name` VARCHAR(2048) NOT NULL,
    `parent_id` BIGINT NULL

) ENGINE=OLAP
DUPLICATE KEY(`node_id`)
DISTRIBUTED BY HASH(`node_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE genetic_alteration (
    `genetic_profile_id` BIGINT NOT NULL,
    `genetic_entity_id` BIGINT NOT NULL,
    `values` VARCHAR(65533) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`genetic_profile_id`)
DISTRIBUTED BY HASH(`genetic_profile_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE genetic_entity (
    `id` BIGINT NOT NULL,
    `entity_type` VARCHAR(2048) NOT NULL,
    `stable_id` VARCHAR(2048) NULL

) ENGINE=OLAP
DUPLICATE KEY(`id`)
DISTRIBUTED BY HASH(`id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE genetic_profile (
    `genetic_profile_id` BIGINT NOT NULL,
    `stable_id` VARCHAR(2048) NOT NULL,
    `cancer_study_id` BIGINT NOT NULL,
    `genetic_alteration_type` VARCHAR(2048) NOT NULL,
    `generic_assay_type` VARCHAR(2048) NULL,
    `datatype` VARCHAR(2048) NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `description` VARCHAR(65533) NULL,
    `show_profile_in_analysis_tab` INT NOT NULL,
    `pivot_threshold` DOUBLE NULL,
    `sort_order` VARCHAR(2048) NULL,
    `patient_level` INT NULL

) ENGINE=OLAP
DUPLICATE KEY(`genetic_profile_id`)
DISTRIBUTED BY HASH(`genetic_profile_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE genetic_profile_link (
    `referring_genetic_profile_id` BIGINT NOT NULL,
    `referred_genetic_profile_id` BIGINT NOT NULL,
    `reference_type` VARCHAR(2048) NULL

) ENGINE=OLAP
DUPLICATE KEY(`referring_genetic_profile_id`)
DISTRIBUTED BY HASH(`referring_genetic_profile_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE genetic_profile_samples (
    `genetic_profile_id` BIGINT NOT NULL,
    `ordered_sample_list` VARCHAR(65533) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`genetic_profile_id`)
DISTRIBUTED BY HASH(`genetic_profile_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE gistic (
    `gistic_roi_id` BIGINT NOT NULL,
    `cancer_study_id` BIGINT NOT NULL,
    `chromosome` BIGINT NOT NULL,
    `cytoband` VARCHAR(2048) NOT NULL,
    `wide_peak_start` BIGINT NOT NULL,
    `wide_peak_end` BIGINT NOT NULL,
    `q_value` DOUBLE NOT NULL,
    `amp` INT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`gistic_roi_id`)
DISTRIBUTED BY HASH(`gistic_roi_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE gistic_to_gene (
    `gistic_roi_id` BIGINT NOT NULL,
    `entrez_gene_id` BIGINT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`gistic_roi_id`)
DISTRIBUTED BY HASH(`gistic_roi_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE info (
    `db_schema_version` VARCHAR(255) NOT NULL,
    `geneset_version` VARCHAR(2048) NULL,
    `derived_table_schema_version` VARCHAR(2048) NULL,
    `gene_table_version` VARCHAR(2048) NULL

) ENGINE=OLAP
DUPLICATE KEY(`db_schema_version`)
DISTRIBUTED BY HASH(`db_schema_version`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE mut_sig (
    `cancer_study_id` BIGINT NOT NULL,
    `entrez_gene_id` BIGINT NOT NULL,
    `rank` BIGINT NOT NULL,
    `NumBasesCovered` BIGINT NOT NULL,
    `NumMutations` BIGINT NOT NULL,
    `p_value` DOUBLE NOT NULL,
    `q_value` DOUBLE NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`cancer_study_id`)
DISTRIBUTED BY HASH(`cancer_study_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE mutation (
    `mutation_event_id` BIGINT NOT NULL COMMENT 'References mutation_event.mutation_event_id.',
    `genetic_profile_id` BIGINT NOT NULL COMMENT 'References genetic_profile.genetic_profile_id.',
    `sample_id` BIGINT NOT NULL COMMENT 'References sample.internal_id.',
    `entrez_gene_id` BIGINT NOT NULL COMMENT 'References gene.entrez_gene_id.',
    `center` VARCHAR(2048) NULL COMMENT 'Center where sequencing was performed.',
    `sequencer` VARCHAR(2048) NULL COMMENT 'Sequencing platform used.',
    `mutation_status` VARCHAR(2048) NULL COMMENT 'Mutation status: Germline,
    Somatic,
    or LOH.',
    `validation_status` VARCHAR(2048) NULL COMMENT 'Validation status.',
    `tumor_seq_allele1` VARCHAR(2048) NULL COMMENT 'Tumor allele 1 sequence.',
    `tumor_seq_allele2` VARCHAR(2048) NULL COMMENT 'Tumor allele 2 sequence.',
    `matched_norm_sample_barcode` VARCHAR(2048) NULL COMMENT 'Matched normal sample barcode.',
    `match_norm_seq_allele1` VARCHAR(2048) NULL COMMENT 'Matched normal allele 1 sequence.',
    `match_norm_seq_allele2` VARCHAR(2048) NULL COMMENT 'Matched normal allele 2 sequence.',
    `tumor_validation_allele1` VARCHAR(2048) NULL COMMENT 'Tumor validation allele 1 sequence.',
    `tumor_validation_allele2` VARCHAR(2048) NULL COMMENT 'Tumor validation allele 2 sequence.',
    `match_norm_validation_allele1` VARCHAR(2048) NULL COMMENT 'Matched normal validation allele 1.',
    `match_norm_validation_allele2` VARCHAR(2048) NULL COMMENT 'Matched normal validation allele 2.',
    `verification_status` VARCHAR(2048) NULL COMMENT 'Verification status.',
    `sequencing_phase` VARCHAR(2048) NULL COMMENT 'Sequencing phase.',
    `sequence_source` VARCHAR(2048) NOT NULL COMMENT 'Source of sequencing data.',
    `validation_method` VARCHAR(2048) NULL COMMENT 'Validation method used.',
    `score` VARCHAR(2048) NULL COMMENT 'Score or quality metric.',
    `bam_file` VARCHAR(2048) NULL COMMENT 'Associated BAM file.',
    `tumor_alt_count` BIGINT NULL COMMENT 'Tumor alternate allele count.',
    `tumor_ref_count` BIGINT NULL COMMENT 'Tumor reference allele count.',
    `normal_alt_count` BIGINT NULL COMMENT 'Normal alternate allele count.',
    `normal_ref_count` BIGINT NULL COMMENT 'Normal reference allele count.',
    `amino_acid_change` VARCHAR(2048) NULL COMMENT 'Amino acid change from mutation.',
    `annotation_json` VARCHAR(65533) NULL COMMENT 'JSON-formatted annotations.'

) ENGINE=OLAP
DUPLICATE KEY(`mutation_event_id`)
DISTRIBUTED BY HASH(`mutation_event_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE mutation_count_by_keyword (
    `genetic_profile_id` BIGINT NOT NULL,
    `keyword` VARCHAR(2048) NULL,
    `entrez_gene_id` BIGINT NOT NULL,
    `keyword_count` BIGINT NOT NULL,
    `gene_count` BIGINT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`genetic_profile_id`)
DISTRIBUTED BY HASH(`genetic_profile_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE mutation_event (
    `mutation_event_id` BIGINT NOT NULL,
    `entrez_gene_id` BIGINT NOT NULL,
    `chr` VARCHAR(2048) NULL,
    `start_position` BIGINT NULL,
    `end_position` BIGINT NULL,
    `reference_allele` VARCHAR(2048) NULL,
    `tumor_seq_allele` VARCHAR(2048) NULL,
    `protein_change` VARCHAR(2048) NULL,
    `mutation_type` VARCHAR(2048) NULL,
    `ncbi_build` VARCHAR(2048) NULL,
    `strand` VARCHAR(2048) NULL,
    `variant_type` VARCHAR(2048) NULL,
    `db_snp_rs` VARCHAR(2048) NULL,
    `db_snp_val_status` VARCHAR(2048) NULL,
    `refseq_mrna_id` VARCHAR(2048) NULL,
    `codon_change` VARCHAR(2048) NULL,
    `uniprot_accession` VARCHAR(2048) NULL,
    `protein_pos_start` BIGINT NULL,
    `protein_pos_end` BIGINT NULL,
    `canonical_transcript` INT NULL,
    `keyword` VARCHAR(2048) NULL

) ENGINE=OLAP
DUPLICATE KEY(`mutation_event_id`)
DISTRIBUTED BY HASH(`mutation_event_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE patient (
    `internal_id` BIGINT NOT NULL,
    `stable_id` VARCHAR(2048) NOT NULL,
    `cancer_study_id` BIGINT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE reference_genome (
    `reference_genome_id` BIGINT NOT NULL,
    `species` VARCHAR(2048) NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `build_name` VARCHAR(2048) NOT NULL,
    `genome_size` BIGINT NULL,
    `url` VARCHAR(2048) NOT NULL,
    `release_date` DATETIME NULL

) ENGINE=OLAP
DUPLICATE KEY(`reference_genome_id`)
DISTRIBUTED BY HASH(`reference_genome_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE reference_genome_gene (
    `entrez_gene_id` BIGINT NOT NULL,
    `reference_genome_id` BIGINT NOT NULL,
    `chr` VARCHAR(2048) NULL,
    `cytoband` VARCHAR(2048) NULL,
    `start` BIGINT NULL,
    `end` BIGINT NULL

) ENGINE=OLAP
DUPLICATE KEY(`entrez_gene_id`)
DISTRIBUTED BY HASH(`entrez_gene_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE resource_definition (
    `resource_id` VARCHAR(255) NOT NULL,
    `display_name` VARCHAR(2048) NOT NULL,
    `description` VARCHAR(65533) NULL,
    `resource_type` VARCHAR(2048) NOT NULL,
    `open_by_default` INT NULL,
    `priority` BIGINT NOT NULL,
    `cancer_study_id` BIGINT NOT NULL,
    `custom_metadata` VARCHAR(65533) NULL

) ENGINE=OLAP
DUPLICATE KEY(`resource_id`)
DISTRIBUTED BY HASH(`resource_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE resource_patient (
    `internal_id` BIGINT NOT NULL,
    `resource_id` VARCHAR(2048) NOT NULL,
    `url` VARCHAR(2048) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE resource_sample (
    `internal_id` BIGINT NOT NULL,
    `resource_id` VARCHAR(2048) NOT NULL,
    `url` VARCHAR(2048) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE resource_study (
    `internal_id` BIGINT NOT NULL,
    `resource_id` VARCHAR(2048) NOT NULL,
    `url` VARCHAR(2048) NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE schema_migrations (
    `version` VARCHAR(64) NOT NULL,
    `description` VARCHAR(255) NOT NULL,
    `installed_on` DATETIME NOT NULL
) ENGINE=OLAP
DUPLICATE KEY(`version`)
DISTRIBUTED BY HASH(`version`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE sample (
    `internal_id` BIGINT NOT NULL,
    `stable_id` VARCHAR(2048) NOT NULL,
    `sample_type` VARCHAR(2048) NOT NULL,
    `patient_id` BIGINT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE sample_cna_event (
    `cna_event_id` BIGINT NOT NULL COMMENT 'References cna_event.cna_event_id.',
    `sample_id` BIGINT NOT NULL COMMENT 'References sample.internal_id.',
    `genetic_profile_id` BIGINT NOT NULL COMMENT 'References genetic_profile.genetic_profile_id.',
    `annotation_json` VARCHAR(65533) NULL COMMENT 'JSON-formatted annotation details.'

) ENGINE=OLAP
DUPLICATE KEY(`cna_event_id`)
DISTRIBUTED BY HASH(`cna_event_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE sample_list (
    `list_id` BIGINT NOT NULL,
    `stable_id` VARCHAR(2048) NOT NULL,
    `category` VARCHAR(2048) NOT NULL,
    `cancer_study_id` BIGINT NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `description` VARCHAR(65533) NULL

) ENGINE=OLAP
DUPLICATE KEY(`list_id`)
DISTRIBUTED BY HASH(`list_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE sample_list_list (
    `list_id` BIGINT NOT NULL,
    `sample_id` BIGINT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`list_id`)
DISTRIBUTED BY HASH(`list_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE sample_profile (
    `sample_id` BIGINT NOT NULL,
    `genetic_profile_id` BIGINT NOT NULL,
    `panel_id` BIGINT NULL

) ENGINE=OLAP
DUPLICATE KEY(`sample_id`)
DISTRIBUTED BY HASH(`sample_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE structural_variant (
    `internal_id` BIGINT NOT NULL,
    `genetic_profile_id` BIGINT NOT NULL,
    `sample_id` BIGINT NOT NULL,
    `site1_entrez_gene_id` BIGINT NULL,
    `site1_ensembl_transcript_id` VARCHAR(2048) NULL,
    `site1_chromosome` VARCHAR(2048) NULL,
    `site1_region` VARCHAR(2048) NULL,
    `site1_region_number` BIGINT NULL,
    `site1_contig` VARCHAR(2048) NULL,
    `site1_position` BIGINT NULL,
    `site1_description` VARCHAR(2048) NULL,
    `site2_entrez_gene_id` BIGINT NULL,
    `site2_ensembl_transcript_id` VARCHAR(2048) NULL,
    `site2_chromosome` VARCHAR(2048) NULL,
    `site2_region` VARCHAR(2048) NULL,
    `site2_region_number` BIGINT NULL,
    `site2_contig` VARCHAR(2048) NULL,
    `site2_position` BIGINT NULL,
    `site2_description` VARCHAR(2048) NULL,
    `site2_effect_on_frame` VARCHAR(2048) NULL,
    `ncbi_build` VARCHAR(2048) NULL,
    `dna_support` VARCHAR(2048) NULL,
    `rna_support` VARCHAR(2048) NULL,
    `normal_read_count` BIGINT NULL,
    `tumor_read_count` BIGINT NULL,
    `normal_variant_count` BIGINT NULL,
    `tumor_variant_count` BIGINT NULL,
    `normal_paired_end_read_count` BIGINT NULL,
    `tumor_paired_end_read_count` BIGINT NULL,
    `normal_split_read_count` BIGINT NULL,
    `tumor_split_read_count` BIGINT NULL,
    `annotation` VARCHAR(65533) NULL,
    `breakpoint_type` VARCHAR(2048) NULL,
    `connection_type` VARCHAR(2048) NULL,
    `event_info` VARCHAR(65533) NULL,
    `class` VARCHAR(2048) NULL,
    `length` BIGINT NULL,
    `comments` VARCHAR(65533) NULL,
    `sv_status` VARCHAR(2048) NOT NULL,
    `annotation_json` VARCHAR(65533) NULL

) ENGINE=OLAP
DUPLICATE KEY(`internal_id`)
DISTRIBUTED BY HASH(`internal_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE type_of_cancer (
    `type_of_cancer_id` VARCHAR(255) NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `dedicated_color` VARCHAR(2048) NOT NULL,
    `short_name` VARCHAR(2048) NULL,
    `parent` VARCHAR(2048) NULL

) ENGINE=OLAP
DUPLICATE KEY(`type_of_cancer_id`)
DISTRIBUTED BY HASH(`type_of_cancer_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

CREATE TABLE users (
    `email` VARCHAR(255) NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `enabled` INT NOT NULL

) ENGINE=OLAP
DUPLICATE KEY(`email`)
DISTRIBUTED BY HASH(`email`) BUCKETS 1
PROPERTIES ("replication_num" = "1");
