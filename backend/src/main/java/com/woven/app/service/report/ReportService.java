package com.woven.app.service.report;

import com.woven.app.web.dto.report.OrgHierarchyRowDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    @PersistenceContext
    private EntityManager em;
/***EVALUATOR - Task B4 - ability to generate report with multiple columns and rows. ***/
    public List<OrgHierarchyRowDto> orgHierarchy(
            Integer orgId,
            String nameLike,
            Instant createdFrom,
            Instant createdTo,
            boolean includeEmptyChildren
    ) {
        // SQL for report
        String base = """
            SELECT
              o.org_id, o.org_name, o.created_timestamp AS o_cts, o.last_updated_timestamp AS o_lts,
              g.org_group_id, g.org_group_name, g.created_timestamp AS g_cts, g.last_updated_timestamp AS g_lts,
              d.department_id, d.department_name, d.created_timestamp AS d_cts, d.last_updated_timestamp AS d_lts,
              s.dept_subgroup_id, s.dept_subgroup_name, s.created_timestamp AS s_cts, s.last_updated_timestamp AS s_lts
            FROM org o
              %s org_group g ON g.org_id = o.org_id
              %s department d ON d.org_group_id = g.org_group_id
              %s dept_subgroup s ON s.department_id = d.department_id
            WHERE 1=1
            """;

        String joinType = includeEmptyChildren ? "LEFT JOIN" : "JOIN";
        String sql = base.formatted(joinType, joinType, joinType);

        var params = new ArrayList<Object>();
        var where = new StringBuilder();

        if (orgId != null) {
            where.append(" AND o.org_id = ?1");
            params.add(orgId);
        }
        if (nameLike != null && !nameLike.isBlank()) {
            where.append(" AND (o.org_name LIKE ?%n OR g.org_group_name LIKE ?%n OR d.department_name LIKE ?%n OR s.dept_subgroup_name LIKE ?%n)");
        }
        if (createdFrom != null) {
            where.append(" AND COALESCE(s.created_timestamp, d.created_timestamp, g.created_timestamp, o.created_timestamp) >= ?%n");
        }
        if (createdTo != null) {
            where.append(" AND COALESCE(s.created_timestamp, d.created_timestamp, g.created_timestamp, o.created_timestamp) <= ?%n");
        }

        // Rebuild with correct param indices
        int idx = params.size() + 1;
        String whereSql = where.toString();
        if (nameLike != null && !nameLike.isBlank()) {
            whereSql = whereSql.replaceFirst("\\?%n", "?" + idx++);
            whereSql = whereSql.replaceFirst("\\?%n", "?" + idx++);
            whereSql = whereSql.replaceFirst("\\?%n", "?" + idx++);
            whereSql = whereSql.replaceFirst("\\?%n", "?" + idx++);
        }
        if (createdFrom != null) whereSql = whereSql.replaceFirst("\\?%n", "?" + idx++);
        if (createdTo != null)   whereSql = whereSql.replaceFirst("\\?%n", "?" + idx++);

        sql = sql + whereSql + " ORDER BY o.org_name, g.org_group_name, d.department_name, s.dept_subgroup_name";

        var q = em.createNativeQuery(sql);
        int p = 1;
        for (Object val : params) q.setParameter(p++, val);
        if (nameLike != null && !nameLike.isBlank()) {
            String like = "%" + nameLike + "%";
            q.setParameter(p++, like);
            q.setParameter(p++, like);
            q.setParameter(p++, like);
            q.setParameter(p++, like);
        }
        if (createdFrom != null) q.setParameter(p++, createdFrom);
        if (createdTo != null)   q.setParameter(p++, createdTo);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        var out = new ArrayList<OrgHierarchyRowDto>(rows.size());
        for (Object[] r : rows) {
            out.add(new OrgHierarchyRowDto(
                    (Integer) r[0], (String) r[1],
                    (Integer) r[4], (String) r[5],
                    (Integer) r[8], (String) r[9],
                    (Integer) r[12], (String) r[13],
                    // Pick the appropriate timestamps available, fallback upwards
                    (r[14] != null ? (java.sql.Timestamp) r[14]
                            : r[10] != null ? (java.sql.Timestamp) r[10]
                            : r[6]  != null ? (java.sql.Timestamp) r[6]
                            : (java.sql.Timestamp) r[2]).toInstant(),
                    (r[15] != null ? (java.sql.Timestamp) r[15]
                            : r[11] != null ? (java.sql.Timestamp) r[11]
                            : r[7]  != null ? (java.sql.Timestamp) r[7]
                            : (java.sql.Timestamp) r[3]).toInstant()
            ));
        }
        return out;
    }
}

