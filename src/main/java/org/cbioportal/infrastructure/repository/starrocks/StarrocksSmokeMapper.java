package org.cbioportal.infrastructure.repository.starrocks;

public interface StarrocksSmokeMapper {

  Integer selectOne();

  String currentVersion();
}
