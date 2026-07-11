package org.cbioportal.infrastructure.repository.starrocks.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Converts StarRocks MAP values returned through the MySQL protocol to Java maps. */
public class StarrocksMapTypeHandler extends BaseTypeHandler<HashMap<String, String>> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<HashMap<String, String>> MAP_TYPE = new TypeReference<>() {};

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, HashMap<String, String> parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setObject(i, parameter);
  }

  @Override
  public HashMap<String, String> getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return convert(rs.getObject(columnName));
  }

  @Override
  public HashMap<String, String> getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return convert(rs.getObject(columnIndex));
  }

  @Override
  public HashMap<String, String> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return convert(cs.getObject(columnIndex));
  }

  private static HashMap<String, String> convert(Object value) throws SQLException {
    if (value == null) {
      return null;
    }
    if (value instanceof Map<?, ?> map) {
      HashMap<String, String> result = new HashMap<>();
      map.forEach(
          (key, item) ->
              result.put(String.valueOf(key), item == null ? null : String.valueOf(item)));
      return result;
    }
    try {
      if (value instanceof byte[] bytes) {
        return OBJECT_MAPPER.readValue(bytes, MAP_TYPE);
      }
      return OBJECT_MAPPER.readValue(String.valueOf(value), MAP_TYPE);
    } catch (IOException exception) {
      throw new SQLException("Unable to decode StarRocks MAP value", exception);
    }
  }
}
