INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time)
VALUES (1, 'xxl-job-executor-sample', '示例执行器', 0, NULL, '2018-11-03 22:21:31');

INSERT INTO xxl_job_info(id, job_group, job_desc, add_time, update_time, author, alarm_email,
                         schedule_type, schedule_conf, misfire_strategy, executor_route_strategy,
                         executor_handler, executor_param, executor_block_strategy, executor_timeout,
                         executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime,
                         child_jobid)
VALUES (1, 1, '测试任务1', '2018-11-03 22:21:31', '2018-11-03 22:21:31', 'XXL', '', 'CRON', '0 0 0 * * ? *',
        'DO_NOTHING', 'FIRST', 'demoJobHandler', '', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化',
        '2018-11-03 22:21:31', '');

INSERT INTO xxl_job_user(id, username, password, role, permission)
VALUES (1, 'admin', '3ddaeb3c569affdae6a96f3784e86919', 1, NULL);

INSERT INTO xxl_job_lock(lock_name)
VALUES ('schedule_lock');

COMMIT;

SELECT setval(pg_get_serial_sequence('xxl_job_group', 'id'), COALESCE((SELECT MAX(id) FROM xxl_job_group), 1), true);
SELECT setval(pg_get_serial_sequence('xxl_job_info', 'id'), COALESCE((SELECT MAX(id) FROM xxl_job_info), 1), true);
SELECT setval(pg_get_serial_sequence('xxl_job_user', 'id'), COALESCE((SELECT MAX(id) FROM xxl_job_user), 1), true);
