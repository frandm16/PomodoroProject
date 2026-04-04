-- Eliminar la foreign key constraint y la columna task_id de la tabla todo_item
ALTER TABLE todo_item DROP COLUMN IF EXISTS task_id CASCADE;