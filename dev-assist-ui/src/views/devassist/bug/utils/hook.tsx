import dayjs from "dayjs";
import {h, onMounted, reactive, ref, toRaw} from "vue";
import {message} from "@/utils/message";
import {addDialog} from "@/components/ReDialog";
import type {PaginationProps} from "@pureadmin/table";
import {useUserStoreHook} from "@/store/modules/user";
import editForm from "../form.vue";
import statusForm from "../statusForm.vue";
import assignForm from "../assignForm.vue";
import attachmentForm from "../attachmentForm.vue";
import {getProjectList, getProjectMembers} from "@/api/devassist/project";
import {getSprintList} from "@/api/devassist/sprint";
import {assignBug, changeBugStatus, createBug, deleteBug, getBugList, updateBug} from "@/api/devassist/bug";

const STATUS_MAP = {
    PENDING_CONFIRM: "待确认",
    PENDING_FIX: "待修复",
    FIXING: "修复中",
    PENDING_VERIFY: "待验证",
    CLOSED: "已关闭",
    REJECTED: "拒绝修复"
};
// el-tag type 空值用 undefined（传 "" 会触发 validator 告警）；NORMAL/MEDIUM/CLOSED 留默认色
const STATUS_TYPE: any = {
    PENDING_CONFIRM: "warning",
    PENDING_FIX: "info",
    FIXING: "primary",
    PENDING_VERIFY: "success",
    REJECTED: "danger"
};
const SEVERITY_MAP = {
    MINOR: "轻微",
    NORMAL: "普通",
    MAJOR: "严重",
    CRITICAL: "致命"
};
const SEVERITY_TYPE: any = {
    MINOR: "info",
    MAJOR: "warning",
    CRITICAL: "danger"
};
const PRIORITY_MAP = {LOW: "低", MEDIUM: "中", HIGH: "高"};
const PRIORITY_TYPE: any = {LOW: "info", HIGH: "warning"};

export function useBug() {
    const form = reactive({
        projectId: "",
        sprintId: "",
        status: "",
        severity: "",
        keyword: "",
        page: 1,
        pageSize: 10
    });
    const formRef = ref();
    const editFormRef = ref();
    const statusFormRef = ref();
    const assignFormRef = ref();
    const attachmentFormRef = ref();
    const dataList = ref([]);
    const loading = ref(true);
    const projectOptions = ref([]);
    const sprintOptions = ref([]);

    const isOwner = (useUserStoreHook().roles || []).includes("OWNER");

    const pagination = reactive<PaginationProps>({
        total: 0,
        pageSize: 10,
        currentPage: 1,
        background: true
    });

    const columns: TableColumnList = [
        {label: "ID", prop: "id", width: 70},
        {label: "缺陷标题", prop: "title", minWidth: 180},
        {
            label: "严重度",
            prop: "severity",
            width: 90,
            cellRenderer: ({row}) => (
                <el-tag effect="plain" type={SEVERITY_TYPE[row.severity]}>
                    {SEVERITY_MAP[row.severity] || row.severity}
                </el-tag>
            )
        },
        {
            label: "优先级",
            prop: "priority",
            width: 80,
            cellRenderer: ({row}) => (
                <el-tag effect="plain" type={PRIORITY_TYPE[row.priority]}>
                    {PRIORITY_MAP[row.priority] || row.priority}
                </el-tag>
            )
        },
        {
            label: "状态",
            prop: "status",
            width: 100,
            cellRenderer: ({row}) => (
                <el-tag effect="plain" type={STATUS_TYPE[row.status]}>
                    {STATUS_MAP[row.status] || row.status}
                </el-tag>
            )
        },
        {label: "提交人", prop: "reporterName", width: 100},
        {label: "修复人", prop: "assigneeName", width: 100},
        {
            label: "创建时间",
            prop: "createTime",
            minWidth: 150,
            formatter: ({createTime}) => dayjs(createTime).format("YYYY-MM-DD HH:mm")
        },
        {label: "操作", fixed: "right", width: 260, slot: "operation"}
    ];

    async function onSearch() {
        loading.value = true;
        try {
            const data: any = await getBugList(toRaw(form));
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
        void onSearch();
    };

    function openDialog(title = "新增", row?: any) {
        addDialog({
            title: `${title}缺陷`,
            props: {
                formInline: {
                    id: row?.id,
                    projectId: row?.projectId ?? "",
                    sprintId: row?.sprintId ?? "",
                    title: row?.title ?? "",
                    severity: row?.severity ?? "NORMAL",
                    priority: row?.priority ?? "MEDIUM",
                    description: row?.description ?? "",
                    stepsToReproduce: row?.stepsToReproduce ?? "",
                    projectOptions: projectOptions.value,
                    sprintOptions: sprintOptions.value
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
                        await createBug(cur);
                        message("提交成功", {type: "success"});
                    } else {
                        await updateBug(cur.id, cur);
                        message("修改成功", {type: "success"});
                    }
                    done();
                    void onSearch();
                });
            }
        });
    }

    function handleChangeStatus(row) {
        addDialog({
            title: `变更状态「${row.title}」`,
            props: {
                formInline: {
                    current: row.status,
                    targetStatus: "",
                    rejectReason: "",
                    fixDescription: "",
                    failReason: ""
                }
            },
            width: "42%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () =>
                h(statusForm, {ref: statusFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                statusFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    await changeBugStatus(row.id, {
                        targetStatus: cur.targetStatus,
                        rejectReason: cur.rejectReason,
                        fixDescription: cur.fixDescription,
                        failReason: cur.failReason
                    });
                    message("状态变更成功", {type: "success"});
                    done();
                    void onSearch();
                });
            }
        });
    }

    async function handleAssign(row) {
        const members: any[] = (await getProjectMembers(row.projectId)) || [];
        addDialog({
            title: `分配修复人「${row.title}」`,
            props: {
                formInline: {
                    assigneeId: row.assigneeId ?? "",
                    memberOptions: members.map((m: any) => ({
                        userId: m.userId,
                        realName: m.realName,
                        projectRole: m.projectRole
                    }))
                }
            },
            width: "40%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () =>
                h(assignForm, {ref: assignFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                assignFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    await assignBug(
                        row.id,
                        cur.assigneeId === "" ? null : cur.assigneeId
                    );
                    message("分配成功", {type: "success"});
                    done();
                    void onSearch();
                });
            }
        });
    }

    function handleAttachment(row) {
        addDialog({
            title: `附件「${row.title}」`,
            props: {
                formInline: {bugId: row.id, bugTitle: row.title}
            },
            width: "56%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () =>
                h(attachmentForm, {ref: attachmentFormRef, formInline: null}),
            beforeSure: done => {
                done();
            }
        });
    }

    async function handleDelete(row) {
        await deleteBug(row.id);
        message("删除成功", {type: "success"});
        void onSearch();
    }

    function handleSizeChange(val: number) {
        form.pageSize = val;
        void onSearch();
    }

    function handleCurrentChange(val: number) {
        form.page = val;
        void onSearch();
    }

    onMounted(async () => {
        const [p, s]: any = await Promise.all([
            getProjectList({pageSize: 100}),
            getSprintList({pageSize: 100})
        ]);
        projectOptions.value = (p?.list || []).map((x: any) => ({
            id: x.id,
            name: x.name
        }));
        sprintOptions.value = (s?.list || []).map((x: any) => ({
            id: x.id,
            name: x.name
        }));
        void onSearch();
    });

    return {
        form,
        formRef,
        loading,
        columns,
        dataList,
        pagination,
        projectOptions,
        sprintOptions,
        isOwner,
        onSearch,
        resetForm,
        openDialog,
        handleChangeStatus,
        handleAssign,
        handleAttachment,
        handleDelete,
        handleSizeChange,
        handleCurrentChange
    };
}
