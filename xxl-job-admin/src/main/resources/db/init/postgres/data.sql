INSERT INTO xxl_job_group (id, app_name, title, address_type, address_list, update_time)
VALUES
    (1, 'xxl-job-executor-sample', '通用执行器Sample', 0, NULL, now()),
    (2, 'xxl-job-executor-sample-ai', 'AI执行器Sample', 0, NULL, now());

INSERT INTO xxl_job_info (
    id,
    job_group,
    job_desc,
    add_time,
    update_time,
    author,
    alarm_email,
    schedule_type,
    schedule_conf,
    misfire_strategy,
    executor_route_strategy,
    executor_handler,
    executor_param,
    executor_block_strategy,
    executor_timeout,
    executor_fail_retry_count,
    glue_type,
    glue_source,
    glue_remark,
    glue_updatetime,
    child_jobid
)
VALUES
    (
        1,
        1,
        '示例任务01',
        now(),
        now(),
        'XXL',
        '',
        'CRON',
        '0 0 0 * * ? *',
        'DO_NOTHING',
        'FIRST',
        'demoJobHandler',
        '',
        'SERIAL_EXECUTION',
        0,
        0,
        'BEAN',
        '',
        'GLUE代码初始化',
        now(),
        ''
    ),
    (
        2,
        2,
        'Ollama示例任务01',
        now(),
        now(),
        'XXL',
        '',
        'NONE',
        '',
        'DO_NOTHING',
        'FIRST',
        'ollamaJobHandler',
        $json$
            {
    "input": "慢SQL问题分析思路",
        "prompt": "你是一个研发工程师，擅长解决技术类问题。",
        "model": "qwen3:0.6b"
}
$json$,
        'SERIAL_EXECUTION',
        0,
        0,
        'BEAN',
        '',
        'GLUE代码初始化',
        now(),
        ''
    ),
    (
        3,
        2,
        'Dify示例任务',
        now(),
        now(),
        'XXL',
        '',
        'NONE',
        '',
        'DO_NOTHING',
        'FIRST',
        'difyWorkflowJobHandler',
        $json$
            {
    "inputs": {
        "input": "查询班级各学科前三名"
    },
        "user": "xxl-job",
        "baseUrl": "http://localhost/v1",
        "apiKey": "app-OUVgNUOQRIMokfmuJvBJoUTN"
}
$json$,
        'SERIAL_EXECUTION',
        0,
        0,
        'BEAN',
        '',
        'GLUE代码初始化',
        now(),
        ''
    );

INSERT INTO xxl_job_user (id, username, password, role, permission)
VALUES (1, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, NULL);

INSERT INTO xxl_job_lock (lock_name)
VALUES ('schedule_lock');

SELECT setval(pg_get_serial_sequence('xxl_job_group', 'id'), COALESCE((SELECT MAX(id) FROM xxl_job_group), 1), true);
SELECT setval(pg_get_serial_sequence('xxl_job_info', 'id'), COALESCE((SELECT MAX(id) FROM xxl_job_info), 1), true);
SELECT setval(pg_get_serial_sequence('xxl_job_user', 'id'), COALESCE((SELECT MAX(id) FROM xxl_job_user), 1), true);
