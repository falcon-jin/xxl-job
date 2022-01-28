/**
 * Copyright (c) 2018-2028, Chill Zhuang 庄骞 (smallchill@163.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.huansi.job.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Minio参数配置类
 *
 * @author Chill
 */
@Data
@ConfigurationProperties(prefix = "hs.job")
public class HsJobProperties {
	/**
	 * 是否启用
	 */
	private Boolean enable;
	/**
	 * 控制台地址
	 */
	private String adminAddresses;
	/**
	 * token
	 */
	private String accessToken;
	/**
	 * 执行器名称
	 */
	private String appName;
	/**
	 * 注册地址 和ip配置一个就行 address优先级较高
	 */
	private String address;
	/**
	 * 注册ip
	 */
	private String ip;
	/**
	 * 注册端口号
	 */
	private int port = 9999;
	/**
	 * 日志存放路径
	 */
	private String logPath;
	/**
	 * 日志保留天数
	 */
	private int logRetentionDays =3;

}
