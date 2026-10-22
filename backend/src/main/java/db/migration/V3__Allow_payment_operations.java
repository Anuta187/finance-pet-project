package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

// В V1 ограничение на type создано без имени, поэтому ищем его в INFORMATION_SCHEMA и пересоздаём
public class V3__Allow_payment_operations extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement st = context.getConnection().createStatement()) {
            List<String> names = new ArrayList<>();
            try (ResultSet rs = st.executeQuery("""
                    SELECT cc.CONSTRAINT_NAME
                    FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc
                    JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                      ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                    WHERE tc.TABLE_NAME = 'OPERATIONS' AND cc.CHECK_CLAUSE LIKE '%OPENING%'""")) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
            }
            for (String name : names) {
                st.execute("ALTER TABLE operations DROP CONSTRAINT \"" + name + "\"");
            }
            st.execute("ALTER TABLE operations ADD CONSTRAINT ck_operations_type "
                    + "CHECK (type IN ('OPENING', 'TRANSFER', 'PAYMENT'))");
            st.execute("ALTER TABLE operations ALTER COLUMN to_account_id SET NULL");
        }
    }
}
