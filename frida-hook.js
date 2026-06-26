// 番茄小说动态Hook脚本
// 功能：监控签到、任务、奖励相关的方法调用

Java.perform(function() {
    console.log("[*] 番茄小说Hook脚本已加载");
    
    // 签到相关类
    var signClasses = [
        "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinRepo",
        "com.dragon.read.ug.kmp.longsignin.viewmodel.LongSignInKlayViewModel",
        "com.dragon.read.ug.kmp.newusersignin.viewmodel.NewUserSevenDaySignInViewModel"
    ];
    
    // 任务相关类
    var taskClasses = [
        "com.dragon.read.ug.kmp.common.repository.TaskDoneDataRepository",
        "com.dragon.read.ug.kmp.treasurebox.repository.TreasureTaskDoneDataRepository",
        "com.dragon.read.ug.kmp.dividegoldcoin.viewmodel.SecondFloorDivideGoldCoinViewModel"
    ];
    
    // Hook签到相关类
    signClasses.forEach(function(className) {
        try {
            var clazz = Java.use(className);
            console.log("[+] 找到签到类: " + className);
            
            // Hook所有方法
            var methods = clazz.class.getDeclaredMethods();
            methods.forEach(function(method) {
                var methodName = method.getName();
                if (methodName.contains("Sign") || methodName.contains("sign") || 
                    methodName.contains("SignIn") || methodName.contains("signIn")) {
                    
                    try {
                        var overloads = clazz[methodName].overloads;
                        overloads.forEach(function(overload) {
                            overload.implementation = function() {
                                console.log("\n[=== 签到方法调用 ===]");
                                console.log("[*] 类: " + className);
                                console.log("[*] 方法: " + methodName);
                                console.log("[*] 参数: " + JSON.stringify(arguments));
                                
                                var result = this[methodName].apply(this, arguments);
                                console.log("[*] 返回: " + result);
                                console.log("[===================]\n");
                                
                                return result;
                            };
                            console.log("[+] Hook签到方法: " + className + "." + methodName);
                        });
                    } catch(e) {
                        // 忽略
                    }
                }
            });
        } catch(e) {
            console.log("[-] 未找到类: " + className);
        }
    });
    
    // Hook任务相关类
    taskClasses.forEach(function(className) {
        try {
            var clazz = Java.use(className);
            console.log("[+] 找到任务类: " + className);
            
            // Hook所有方法
            var methods = clazz.class.getDeclaredMethods();
            methods.forEach(function(method) {
                var methodName = method.getName();
                if (methodName.contains("Task") || methodName.contains("task") || 
                    methodName.contains("Done") || methodName.contains("done") ||
                    methodName.contains("Reward") || methodName.contains("reward")) {
                    
                    try {
                        var overloads = clazz[methodName].overloads;
                        overloads.forEach(function(overload) {
                            overload.implementation = function() {
                                console.log("\n[=== 任务方法调用 ===]");
                                console.log("[*] 类: " + className);
                                console.log("[*] 方法: " + methodName);
                                console.log("[*] 参数: " + JSON.stringify(arguments));
                                
                                var result = this[methodName].apply(this, arguments);
                                console.log("[*] 返回: " + result);
                                console.log("[===================]\n");
                                
                                return result;
                            };
                            console.log("[+] Hook任务方法: " + className + "." + methodName);
                        });
                    } catch(e) {
                        // 忽略
                    }
                }
            });
        } catch(e) {
            console.log("[-] 未找到类: " + className);
        }
    });
    
    // Hook HTTP请求
    try {
        var OkHttpClient = Java.use("okhttp3.OkHttpClient");
        var Request = Java.use("okhttp3.Request");
        
        // Hook Request.Builder.build
        var RequestBuilder = Java.use("okhttp3.Request$Builder");
        RequestBuilder.build.implementation = function() {
            var request = this.build();
            var url = request.url().toString();
            
            if (url.contains("sign") || url.contains("task") || url.contains("reward") || 
                url.contains("luckycat") || url.contains("coin")) {
                console.log("\n[=== HTTP请求 ===]");
                console.log("[*] URL: " + url);
                console.log("[*] 方法: " + request.method());
                console.log("[================]\n");
            }
            
            return request;
        };
        console.log("[+] Hook OkHttp Request.Builder 成功");
    } catch(e) {
        console.log("[-] Hook OkHttp失败: " + e);
    }
    
    // Hook JSON解析
    try {
        var JSONObject = Java.use("org.json.JSONObject");
        
        // Hook optInt
        JSONObject.optInt.overload('java.lang.String', 'int').implementation = function(key, defaultValue) {
            var value = this.optInt(key, defaultValue);
            
            if (key.contains("status") || key.contains("coin") || key.contains("reward") || 
                key.contains("task") || key.contains("sign")) {
                console.log("[JSON] optInt: " + key + " = " + value);
            }
            
            return value;
        };
        
        // Hook optString
        JSONObject.optString.overload('java.lang.String', 'java.lang.String').implementation = function(key, defaultValue) {
            var value = this.optString(key, defaultValue);
            
            if (key.contains("status") || key.contains("coin") || key.contains("reward") || 
                key.contains("task") || key.contains("sign") || key.contains("name")) {
                console.log("[JSON] optString: " + key + " = " + value);
            }
            
            return value;
        };
        
        console.log("[+] Hook JSONObject 成功");
    } catch(e) {
        console.log("[-] Hook JSONObject失败: " + e);
    }
    
    // Hook Retrofit接口
    try {
        // 尝试找到API接口类
        var apiClasses = [
            "com.dragon.read.rpc.rpc.UgcApiService",
            "com.dragon.read.saas.ugc.rpc.CommentApiService"
        ];
        
        apiClasses.forEach(function(className) {
            try {
                var clazz = Java.use(className);
                console.log("[+] 找到API类: " + className);
                
                var methods = clazz.class.getDeclaredMethods();
                methods.forEach(function(method) {
                    var methodName = method.getName();
                    if (methodName.contains("sign") || methodName.contains("task") || 
                        methodName.contains("reward") || methodName.contains("done")) {
                        
                        try {
                            var overloads = clazz[methodName].overloads;
                            overloads.forEach(function(overload) {
                                overload.implementation = function() {
                                    console.log("\n[=== API调用 ===]");
                                    console.log("[*] 类: " + className);
                                    console.log("[*] 方法: " + methodName);
                                    console.log("[*] 参数: " + JSON.stringify(arguments));
                                    
                                    var result = this[methodName].apply(this, arguments);
                                    console.log("[*] 返回: " + result);
                                    console.log("[==============]\n");
                                    
                                    return result;
                                };
                                console.log("[+] Hook API方法: " + className + "." + methodName);
                            });
                        } catch(e) {
                            // 忽略
                        }
                    }
                });
            } catch(e) {
                console.log("[-] 未找到API类: " + className);
            }
        });
    } catch(e) {
        console.log("[-] Hook Retrofit失败: " + e);
    }
    
    console.log("[*] Hook脚本初始化完成");
    console.log("[*] 请在番茄小说中操作，观察日志输出...");
});
