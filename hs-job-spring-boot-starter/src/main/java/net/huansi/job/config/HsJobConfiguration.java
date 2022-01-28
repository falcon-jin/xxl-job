
package net.huansi.job.config;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.huansi.job.core.executor.impl.HsJobSpringExecutor;
import net.huansi.job.props.HsJobProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.annotation.Bean;

import java.util.Objects;

/**
 * Alioss配置类
 *
 * @author Chill
 */
//@Configuration(proxyBeanMethods = false)
@AllArgsConstructor
@EnableConfigurationProperties(HsJobProperties.class)
@ConditionalOnProperty(value = "hs.job.enable", havingValue = "true",matchIfMissing = false)
@Slf4j
public class HsJobConfiguration {

	private HsJobProperties ossProperties;
	private InetUtils inetUtils;

	@Bean
	public HsJobSpringExecutor xxlJobExecutor() {

		log.info(">>>>>>>>>>> xxl-job config init.");
		HsJobSpringExecutor xxlJobSpringExecutor = new HsJobSpringExecutor();
		xxlJobSpringExecutor.setAdminAddresses(ossProperties.getAdminAddresses());
		String appName = ossProperties.getAppName();
		if(Objects.isNull(appName)||"".equals(appName)){
			appName = System.getProperty("spring.application.name");
		}
		xxlJobSpringExecutor.setAppName(appName);
		xxlJobSpringExecutor.setAddress(ossProperties.getAddress());
		String ip = ossProperties.getIp();
		if(Objects.isNull(ip)||"".equals(ip)){
			ip = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
		}
		xxlJobSpringExecutor.setIp(ip);
		xxlJobSpringExecutor.setPort(ossProperties.getPort());
		xxlJobSpringExecutor.setAccessToken(ossProperties.getAccessToken());
		xxlJobSpringExecutor.setLogPath(ossProperties.getLogPath());
		xxlJobSpringExecutor.setLogRetentionDays(ossProperties.getLogRetentionDays());
		return xxlJobSpringExecutor;
	}

	/**
	 * 针对多网卡、容器内部署等情况，可借助 "spring-cloud-commons" 提供的 "InetUtils" 组件灵活定制注册IP；
	 *
	 *      1、引入依赖：
	 *          <dependency>
	 *             <groupId>org.springframework.cloud</groupId>
	 *             <artifactId>spring-cloud-commons</artifactId>
	 *             <version>${version}</version>
	 *         </dependency>
	 *
	 *      2、配置文件，或者容器启动变量
	 *          spring.cloud.inetutils.preferred-networks: 'xxx.xxx.xxx.'
	 *
	 *      3、获取IP
	 *          String ip_ = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
	 */
}
