/**
 * 组合根：把分散在各业务模块的端口与实现装配起来。
 *
 * <p>只允许依赖各模块对外暴露的 api 命名接口，不访问模块内部包。
 * 这样做是为了让 {@code platform} 共享层保持对业务模块的无知，
 * 跨模块装配集中在这一个包里。
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"kernel", "goal::api", "commerce::api"})
package com.lingxi.wiring;
