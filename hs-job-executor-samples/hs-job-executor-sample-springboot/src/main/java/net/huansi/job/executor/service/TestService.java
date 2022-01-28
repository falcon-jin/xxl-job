package net.huansi.job.executor.service;

import org.springframework.stereotype.Service;

@Service
public class TestService {
    public String test(){
        return System.currentTimeMillis()+"";
    }
}
