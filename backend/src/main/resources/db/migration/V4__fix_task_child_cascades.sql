DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        JOIN pg_attribute attr ON attr.attrelid = rel.oid AND attr.attnum = ANY (con.conkey)
        WHERE con.contype = 'f'
          AND nsp.nspname = current_schema()
          AND rel.relname = 'scheduled_sessions'
          AND attr.attname = 'task_id'
    LOOP
        EXECUTE format('ALTER TABLE scheduled_sessions DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    ALTER TABLE scheduled_sessions
        ADD CONSTRAINT fk_scheduled_sessions_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE;

    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        JOIN pg_attribute attr ON attr.attrelid = rel.oid AND attr.attnum = ANY (con.conkey)
        WHERE con.contype = 'f'
          AND nsp.nspname = current_schema()
          AND rel.relname = 'deadlines'
          AND attr.attname = 'task_id'
    LOOP
        EXECUTE format('ALTER TABLE deadlines DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    ALTER TABLE deadlines
        ADD CONSTRAINT fk_deadlines_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE;
END $$;
