# Mirai Rss订阅插件

基于[Mirai框架](https://github.com/mamoe/mirai)，自动推送更新内容到QQ

# 截图

![](https://pic.rmb.bdstatic.com/bjh/fe21f012edc545460c9e4e5a6414b0e4.png)

# 使用的开源库

- okhttp
- DOM4j
- Fastjson
- Jsoup

# 安装方法

1. 将打包完成的 jar 文件放入 `<Mirai Console Loader>\plugins\目录`
2. 先启动一次 mcl，等待RssBot插件加载完成后关闭
3. 找到 `<Mirai Console Loader>\config\RssBot\config.json`
4. 将 `botId` 改为自己的机器人QQ
5. 重新启动 mcl

# 配置说明

配置文件在 `<Mirai Console Loader>\config\RssBot\config.json`

| 参数                        | 默认值      | 可选值       | 备注                                       |
| --------------------------- | ----------- | ------------ | ------------------------------------------ |
| autoAcceptFriendApplication | true        | true \ false | 自动同意好友申请                           |
| autoAcceptGroupApplication  | true        | true \ false | 自动同意群邀请                             |
| botId                       | "123456789" |              | 机器人QQ号                                 |
| maxSub                      | 100         |              | 最大订阅数量                               |
| proxy_type                  | ""          |              | 代理类型                                   |
| proxy_address               | ""          |              | 代理地址                                   |
| proxy_port                  | ""          |              | 代理端口                                   |
| proxy_username              | ""          |              | 代理用户名                                 |
| proxy_password              | ""          |              | 代理密码                                   |
| enableWhiteList             | false       | true \ false | 开启白名单，只有白名单内的用户能使用机器人 |
| whiteList                   | []          |              | 白名单                                     |
| groupPermissionRestrictions | true        | true \ false | 群内是否只有管理员及群主能使用机器人       |

# 使用方法

## `#sub <url> [interval(minute)]`

增加订阅

| 参数     | 必须 | 备注                       |
| -------- | ---- | -------------------------- |
| url      | 是   | Rss链接                    |
| interval | 否   | 抓取时间间隔（单位：分钟） |

## `#unsub <id>`

取消订阅

| 参数     | 必须 | 备注                       |
| -------- | ---- | -------------------------- |
| id      | 是   |                     |

## `#setinterval <id> <interval(minute)>`

设置抓取时间间隔

| 参数     | 必须 | 备注                       |
| -------- | ---- | -------------------------- |
| id      | 是   |                    |
| interval | 是   | 抓取时间间隔（单位：分钟） |

## `#list`

查询订阅列表

## `#detail <id>`

查询订阅详情

| 参数     | 必须 | 备注                       |
| -------- | ---- | -------------------------- |
| id      | 是   |                     |
