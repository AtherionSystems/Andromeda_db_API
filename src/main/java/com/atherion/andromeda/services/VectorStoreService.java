package com.atherion.andromeda.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    static final int VECTOR_SIZE = 3072;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void upsert(String type, Long entityId, Long projectId, float[] vector, String text) {
        String id = UUID.nameUUIDFromBytes(
                (type + ":" + entityId).getBytes(StandardCharsets.UTF_8)
        ).toString();
        String vectorStr = Arrays.toString(vector);

        try {
            jdbcTemplate.update("DELETE FROM andromeda_vectors WHERE id = ?", id);

            jdbcTemplate.update(
                    "INSERT INTO andromeda_vectors (id, entity_type, entity_id, project_id, text_content, embedding, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, TO_VECTOR(?, 3072, FLOAT32), SYSTIMESTAMP)",
                    ps -> {
                        ps.setString(1, id);
                        ps.setString(2, type);
                        ps.setLong(3, entityId);
                        if (projectId != null) ps.setLong(4, projectId);
                        else ps.setNull(4, Types.NUMERIC);
                        ps.setString(5, text);
                        ps.setClob(6, new StringReader(vectorStr));
                    }
            );
        } catch (Exception e) {
            log.error("Vector upsert failed for {}:{} — {}", type, entityId, e.getMessage());
        }
    }

    public List<String> search(float[] vector, Long projectId, double maxDistance) {
        String vectorStr = Arrays.toString(vector);

        if (projectId != null) {
            return jdbcTemplate.query(
                    "SELECT text_content FROM (" +
                    "  SELECT text_content, VECTOR_DISTANCE(embedding, TO_VECTOR(?, 3072, FLOAT32), COSINE) AS dist" +
                    "  FROM andromeda_vectors WHERE project_id = ?" +
                    ") WHERE dist < ? ORDER BY dist FETCH FIRST 10 ROWS ONLY",
                    ps -> {
                        ps.setClob(1, new StringReader(vectorStr));
                        ps.setLong(2, projectId);
                        ps.setDouble(3, maxDistance);
                    },
                    (rs, rowNum) -> rs.getString("text_content")
            );
        }

        return jdbcTemplate.query(
                "SELECT text_content FROM (" +
                "  SELECT text_content, VECTOR_DISTANCE(embedding, TO_VECTOR(?, 3072, FLOAT32), COSINE) AS dist" +
                "  FROM andromeda_vectors" +
                ") WHERE dist < ? ORDER BY dist FETCH FIRST 10 ROWS ONLY",
                ps -> {
                    ps.setClob(1, new StringReader(vectorStr));
                    ps.setDouble(2, maxDistance);
                },
                (rs, rowNum) -> rs.getString("text_content")
        );
    }

    @Transactional
    public void delete(String type, Long entityId) {
        String id = UUID.nameUUIDFromBytes(
                (type + ":" + entityId).getBytes(StandardCharsets.UTF_8)
        ).toString();
        jdbcTemplate.update("DELETE FROM andromeda_vectors WHERE id = ?", id);
    }
}
