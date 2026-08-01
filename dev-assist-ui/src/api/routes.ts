// 后端不下发动态路由，菜单走前端静态 router/modules（constantMenus）。
// 返回 code:0 触发 initRouter 的 handleAsyncRoutes([]) → handleWholeMenus 用静态菜单组装。
export const getAsyncRoutes = () => {
    return Promise.resolve({code: 0, data: []});
};
