我要落地一个独立的领域事件框架，这个想法是基于项目（/Users/sulin/code/somew/marketing/marketing-core 核心包目录：com.sinew.marketing.core.domain.event）演变和抽象复用出来的，你有需要可以阅读源码。
请先阅读这三份文档并严格以它们为基线设计和生成代码：                                                                                                                                           
                                                                                                                                                                                                                                           
  1. ./docs/domain-event-framework-design.md                                                                                                                                                
  2. ./docs/domain-event-framework-rollout.md                                                                                                                                               
  3. ./docs/domain-event-framework-skeleton-zh.md                                                                                                                                           
                                                                                                                                                                                                                                           
  目标：
  - 当先项目的base包名：com.sulin.codepose.event.framework 不要参考文档的com.sinew.marketing.core.domain.event
  - 先做单模块版本                                                                                                                                                                                                                         
  - 先落地 api/core/spring 骨架                                                                                                                                                                                                            
  - 不做具体业务域实现                                                                                                                                                                                                                     
  - 代码尽量贴近文档中的命名和分层