# Mirai Rss订阅插件

基于[Mirai框架](https://github.com/mamoe/mirai)，自动推送更新内容到QQ群

# 截图

![](https://pic.rmb.bdstatic.com/bjh/fe21f012edc545460c9e4e5a6414b0e4.png)

# 使用的开源库

- DOM4j
- Fastjson

# 安装方法

将打包完成的 jar 文件放入 `<Mirai Console Loader>\plugins\`目录  
启动一次 mcl  
找到 `<Mirai Console Loader>\config\RssBot\data.json`  
将 `botId` 改为已登录的机器人QQ

# 使用方法

`#sub <url> [interval(minute)]`  
增加新订阅  
参数：  
- url - 必填 - RssUrl  
- interval - 选填 - 抓取时间间隔（单位：分钟）

`#unsub <id>`  
禁用订阅  
参数：
- id - 必填 - 订阅对应ID

`#setinterval <id> <interval(minute)>`  
设置抓取时间间隔  
参数：
- id - 必填 - 订阅对应ID
- 时间间隔 - 必填 - 抓取时间间隔（单位：分钟）

`#list`  
订阅列表

`#detail <id>`
订阅详情  
参数：
- id - 必填 - 订阅对应ID
