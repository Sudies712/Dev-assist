import dayjs from "dayjs";
import {h, onMounted, reactive, ref, toRaw} from "vue";
import {ElMessageBox} from "element-plus";
import {message} from "@/utils/message";
import {addDialog} from "@/components/ReDialog";
import type {PaginationProps} from "@pureadmin/table";
import editForm from "../form.vue";
import roleAssignForm from "../roleAssignForm.vue";
import {
    assignUserRoles,
    changeUserStatus,
    createUser,
    getRoleList,
    getUserList,
    resetUserPassword,
    updateUser
} from "@/api/system";

export function useUser() {
    const form = reactive({
        username: "",
        status: "",
        roleId: "",
        page: 1,
        pageSize: 10
    });
    const formRef = ref();
    const editFormRef = ref();
    const roleFormRef = ref();
    const dataList = ref([]);
    const loading = ref(true);
    const roleOptions = ref([]);

    const pagination = reactive<PaginationProps>({
        total: 0,
        pageSize: 10,
        currentPage: 1,
        background: true
    });

    const columns: TableColumnList = [
        {label: "ID", prop: "id", width: 70},
        {label: "用户名", prop: "username", width: 120},
        {label: "姓名", prop: "realName", width: 110},
        {label: "邮箱", prop: "email", minWidth: 160},
        {label: "手机", prop: "phone", width: 130},
        {
            label: "状态",
            width: 100,
            cellRenderer: ({row}) => (
                <el-switch
                    model-value={row.status === "ENABLED"}
                    inline-prompt
                    active-text="启用"
                    inactive-text="禁用"
                    onChange={() => handleStatusChange(row)}
                />
            )
        },
        {
            label: "最近登录",
            prop: "lastLoginTime",
            minWidth: 150,
            formatter: ({lastLoginTime}) =>
                lastLoginTime ? dayjs(lastLoginTime).format("YYYY-MM-DD HH:mm") : "-"
        },
        {label: "操作", fixed: "right", width: 230, slot: "operation"}
    ];

    async function onSearch() {
        loading.value = true;
        try {
            const data: any = await getUserList(toRaw(form));
            dataList.value = data?.list || [];
            pagination.total = data?.total || 0;
            pagination.pageSize = data?.pageSize || 10;
            pagination.currentPage = data?.currentPage || 1;
        } finally {
            loading.value = false;
        }
    }

    const resetForm = el => {
        if (!el) return;
        el.resetFields();
        onSearch();
    };

    function openDialog(title = "新增", row?: any) {
        addDialog({
            title: `${title}用户`,
            props: {
                formInline: {
                    id: row?.id,
                    username: row?.username ?? "",
                    password: "",
                    realName: row?.realName ?? "",
                    email: row?.email ?? "",
                    phone: row?.phone ?? "",
                    roleIds: [],
                    roleOptions: roleOptions.value
                }
            },
            width: "46%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () => h(editForm, {ref: editFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                editFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    if (title === "新增") {
                        await createUser(cur);
                        message("新增成功", {type: "success"});
                    } else {
                        await updateUser(cur.id, {
                            realName: cur.realName,
                            email: cur.email,
                            phone: cur.phone
                        });
                        message("修改成功", {type: "success"});
                    }
                    done();
                    onSearch();
                });
            }
        });
    }

    async function handleStatusChange(row) {
        const target = row.status === "ENABLED" ? "DISABLED" : "ENABLED";
        try {
            await changeUserStatus(row.id, target);
            row.status = target;
            message(target === "ENABLED" ? "已启用" : "已禁用", {type: "success"});
        } catch {
            // 拦截器已提示，el-switch 自动回弹
        }
    }

    async function handleResetPassword(row) {
        const pwd: any = await resetUserPassword(row.id);
        ElMessageBox.alert(
            `用户「${row.username}」的密码已重置，新密码：\n\n\t${pwd}\n\n请妥善转交用户。`,
            "重置密码成功",
            {type: "success", confirmButtonText: "知道了"}
        );
    }

    function handleAssignRoles(row) {
        addDialog({
            title: `分配角色「${row.username}」`,
            props: {
                formInline: {roleIds: [], roleOptions: roleOptions.value}
            },
            width: "42%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () =>
                h(roleAssignForm, {ref: roleFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                if (!cur.roleIds?.length) {
                    message("请至少选择一个角色", {type: "warning"});
                    return;
                }
                assignUserRoles(row.id, cur.roleIds).then(() => {
                    message("角色分配成功", {type: "success"});
                    done();
                });
            }
        });
    }

    function handleSizeChange(val: number) {
        form.pageSize = val;
        onSearch();
    }

    function handleCurrentChange(val: number) {
        form.page = val;
        onSearch();
    }

    onMounted(async () => {
        const roles: any = await getRoleList();
        roleOptions.value = roles || [];
        onSearch();
    });

    return {
        form,
        formRef,
        loading,
        columns,
        dataList,
        pagination,
        roleOptions,
        onSearch,
        resetForm,
        openDialog,
        handleStatusChange,
        handleResetPassword,
        handleAssignRoles,
        handleSizeChange,
        handleCurrentChange
    };
}
