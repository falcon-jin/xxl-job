package com.xxl.job.admin.core.db;

import java.net.URI;
import java.net.URISyntaxException;

public class PostgresJdbcUrlParser {

    private static final String PREFIX = "jdbc:postgresql:";

    private PostgresJdbcUrlParser() {
    }

    public static PostgresJdbcTarget parse(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Unsupported JDBC URL: " + jdbcUrl);
        }

        try {
            URI uri = new URI(jdbcUrl.substring("jdbc:".length()));
            String path = uri.getPath();
            String database = path != null && path.startsWith("/") ? path.substring(1) : path;
            if (database == null || database.isEmpty()) {
                throw new IllegalArgumentException("Database name is missing in JDBC URL: " + jdbcUrl);
            }

            String schema = "public";
            String query = uri.getQuery();
            if (query != null && !query.isEmpty()) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length == 2 && "currentSchema".equals(keyValue[0]) && !keyValue[1].isEmpty()) {
                        schema = keyValue[1];
                        break;
                    }
                }
            }

            return new PostgresJdbcTarget(database, schema);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + jdbcUrl, e);
        }
    }
}
