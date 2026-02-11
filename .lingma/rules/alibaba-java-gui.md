# 阿里巴巴Java开发规范（通义灵码版）

## 一、命名规约
- 类名使用UpperCamelCase，如：UserServiceImpl
- 方法名、参数名、成员变量、局部变量使用lowerCamelCase
- 常量命名全部大写，单词间下划线分隔，如：MAX_COUNT
- 抽象类命名以Abstract或Base开头，异常类以Exception结尾

## 二、OOP规约
- 所有POJO类属性必须使用包装类型（Integer而非int）
- 类成员访问控制：private &gt; protected &gt; public
- 覆写方法必须加@Override注解
- 禁止在POJO中同时存在isXxx()和getXxx()
- 构造方法禁止加入业务逻辑，初始化放在init方法

## 三、并发处理
- 线程资源必须通过线程池提供，禁止自行显式创建线程
- 线程池创建使用ThreadPoolExecutor，禁止使用Executors快捷方式
- SimpleDateFormat必须放在ThreadLocal中使用
- 高并发时同步调用考虑锁粒度，避免对整个方法加锁

## 四、集合处理
- HashMap初始化时指定容量：(预期大小/0.75F+1)
- 使用Map的keySet()、values()、entrySet()返回对象时不可修改
- ArrayList的subList结果不可强转成ArrayList
- 使用集合转数组必须使用toArray(T[] array)带参方法

## 五、控制语句
- 在if/else/for/while/do语句中必须使用大括号
- 多层嵌套不超过3层，复杂逻辑封装成方法
- 禁止在条件判断中执行复杂语句（如赋值、SQL调用）
- 循环体中的语句要考量性能，定义对象、变量、获取数据库连接尽量移至循环体外

## 六、注释规范
- 类、类属性、类方法必须使用Javadoc规范
- 方法注释包含：功能描述、参数说明、返回值、异常说明
- 所有抽象方法必须用Javadoc注释
- 修改代码同时修改对应注释

## 七、异常处理
- 禁止在finally块中使用return
- 捕获异常必须处理，禁止空catch块
- 使用try-with-resources自动关闭资源
- 异常信息必须包含上下文（参数值、业务场景）

## 八、日志规范
- 使用SLF4J API，禁止直接使用System.out.println
- 日志占位符使用{}，禁止用字符串拼接
- 生产环境禁止输出debug日志
- 异常日志必须打印堆栈信息：log.error("xxx", e)

## 九、数据库规约
- 表名、字段名使用小写加下划线（user_name）
- 索引命名：主键pk_，唯一索引uk_，普通索引idx_
- 小数使用DECIMAL，禁止用FLOAT/DOUBLE
- VARCHAR长度不超过5000，超长用TEXT并独立表

## 十、其他
- 禁止使用魔法值（硬编码数字/字符串），必须定义常量
- 长整型常量使用L后缀（1L），禁止用小写l
- 序列化类必须显式声明serialVersionUID
- 单元测试类命名：被测类名+Test，如UserServiceTest