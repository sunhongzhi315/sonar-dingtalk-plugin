import com.zyb.infos.bpm.util.ApplicationContextUtil
import groovyx.net.http.HTTPBuilder

import java.security.MessageDigest

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST

def igwayHost = ApplicationContextUtil.getApplicationContext().getEnvironment().resolvePlaceholders('${zyb.infos.gateway.server}');
def igwayHttp = new HTTPBuilder(igwayHost);
def appId = ApplicationContextUtil.getApplicationContext().getEnvironment().resolvePlaceholders('${zyb.infos.budget.appId}');
def secret = ApplicationContextUtil.getApplicationContext().getEnvironment().resolvePlaceholders('${zyb.infos.budget.appSecret}');


igwayHttp.log.info("budgetDeptNo:" + budgetDeptNo + ",costCode:" + costCode);

def unitCode = ''
def accountCode = ''
igwayHttp.request(POST, JSON) {
    // 时间戳
    def timestamp = (new Date().time / 1000).intValue();
    // 随机串
    def nonceStr = UUID.randomUUID().toString().replaceAll("-", "");
    igwayHttp.log.info("budgetDeptNo:" + budgetDeptNo);
    uri.path = '/budget/api/out/baseBudget/v1/queryDeptToUnit';
    def stringSign = "appId=" + appId + "&nonceStr=" + nonceStr + "&timestamp=" + timestamp + "&secret=" + secret;
    def sign = MessageDigest.getInstance("MD5").digest(stringSign.bytes).encodeHex().toString().toUpperCase();
    uri.query = [appId: appId, timestamp: timestamp, nonceStr: nonceStr, sign: sign];
    def calendar = Calendar.getInstance()
    def currentYear = calendar.get(Calendar.YEAR);
    igwayHttp.log.info("currentYear:" + currentYear);
    body = [budgetYear: currentYear, deptNo: budgetDeptNo];
	// 随便找个地方写一下测试，编辑文件发送 webhook
    response.success = { resp, json ->
				igwayHttp.log.info("queryDeptToUnit->json:" + json);
        def data = json.data;
        if (data.size == 0){
            return result;
        }
        def deptUnit = data.get(0)
        unitCode = deptUnit['budgetUnitCode']
    }
    response.'404' = {
        result.success = false
        result.msg = "404 error"
    }

    // 未根据响应码指定的失败处理闭包
    response.failure = {
        result.success = false
        result.msg = "request error"
    }
}

igwayHttp.request(POST, JSON) {
    // 时间戳
    def timestamp = (new Date().time / 1000).intValue();
    // 随机串
    def nonceStr = UUID.randomUUID().toString().replaceAll("-", "");
    uri.path = '/budget/api/out/baseBudget/v1/queryAccountToCost';
    def stringSign = "appId=" + appId + "&nonceStr=" + nonceStr + "&timestamp=" + timestamp + "&secret=" + secret;
    def sign = MessageDigest.getInstance("MD5").digest(stringSign.bytes).encodeHex().toString().toUpperCase();
    uri.query = [appId: appId, timestamp: timestamp, nonceStr: nonceStr, sign: sign];
    def calendar = Calendar.getInstance()
    def budgetYear = calendar.get(Calendar.YEAR);
    igwayHttp.log.info("budgetYear:" + budgetYear);
    body = [budgetYear: budgetYear, budgetUnitCode: unitCode, costAccountCode: costCode];
    response.success = { resp, json ->
				igwayHttp.log.info("queryAccountToCost->json:" + json);
        result.success = (json.code == 0);
        result.msg = json.msg;
        if (json.code == 0) {
            def data = json.data;
            if (data.size == 0){
                return result;
            }
            def costAccount = data.get(0)
            accountCode = costAccount['budgetAccountCode']
        }
    }
    response.'404' = {
        result.success = false
        result.msg = "404 error"
    }

    // 未根据响应码指定的失败处理闭包
    response.failure = {
        result.success = false
        result.msg = "request error"
    }
}
if(budgetDeptNo!='' && unitCode!='' && accountCode!=''){
	igwayHttp.request(GET, JSON) {
			// 时间戳
			def timestamp = (new Date().time / 1000).intValue();
			// 随机串
			def nonceStr = UUID.randomUUID().toString().replaceAll("-", "");
			igwayHttp.log.info("budgetDeptNo:" + budgetDeptNo + ",budgetUnitCode:" + unitCode + ",budgetAccountCode" + accountCode);
			uri.path = '/budget/api/out/getProTypeByBpm';
			def stringSign = "appId=" + appId + "&budgetAccountCode="+accountCode+"&budgetDeptNo=" + budgetDeptNo + "&budgetUnitCode="+unitCode +"&nonceStr=" + nonceStr + "&timestamp=" + timestamp + "&secret=" + secret;
			def sign = MessageDigest.getInstance("MD5").digest(stringSign.bytes).encodeHex().toString().toUpperCase();
			uri.query = [appId: appId, timestamp: timestamp, nonceStr: nonceStr, sign: sign, budgetDeptNo:budgetDeptNo,budgetUnitCode:unitCode,budgetAccountCode:accountCode];
			response.success = { resp, json ->
					igwayHttp.log.info("getProTypeByBpm->json:" + json);
					result.success = (json.code == 0);
					result.msg = json.msg;
					def data = json.data;
					if (data==null || data.size() == 0){
							result.data.put('readOnly',true)
							return
					}else {
							result.data.put('readOnly',false)
							return
					}
			}
			response.'404' = {
					result.success = false
					result.msg = "404 error"
			}

			// 未根据响应码指定的失败处理闭包
			response.failure = {
					result.success = false
					result.msg = "request error"
			}
	}
}else{
	result.data.put('readOnly',true)
}
