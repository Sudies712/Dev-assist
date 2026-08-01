import dayjs from "dayjs";
import {h, onBeforeUnmount, onMounted, reactive, ref, toRaw} from "vue";
import {message} from "@/utils/message";
import {addDialog} from "@/components/ReDialog";
import type {PaginationProps} from "@pureadmin/table";
import uploadForm from "../uploadForm.vue";
import editForm from "../editForm.vue";
import chunksForm from "../chunksForm.vue";
import {getProjectList} from "@/api/devassist/project";
import {
    deleteDocument,
    downloadDocument,
    getDocumentList,
    reparseDocument,
    updateDocument,
    uploadDocument
} from "@/api/devassist/document";

const TYPE_MAP = {
    REQUIREMENT: "需求",
    DESIGN: "设计",
    API: "接口",
    TEST: "测试",
    MEETING: "会议",
    STANDARD: "规范",
    SPRINT_SUMMARY: "迭代总结",
    PROJECT_SUMMARY: "项目总结",
    OTHER: "其他"
};
const STATUS_MAP = {
    UNPARSED: "未解析",
    PARSING: "解析中",
    PARSED: "已解析",
    FAILED: "解析失败"
};
const STATUS_TYPE: any = {
    UNPARSED: "info",
    PARSING: "warning",
    PARSED: "success",
    FAILED: "danger"
};

export function useDocument() {
    const form = reactive({
        projectId: "",
        type: "",
        parseStatus: "",
        keyword: "",
        page: 1,
        pageSize: 10
    });
    const formRef = ref();
    const uploadFormRef = ref();
    const editFormRef = ref();
    const chunksFormRef = ref();
    const dataList = ref([]);
    const loading = ref(true);
    const projectOptions = ref([]);
    const parseTimer = ref<any>(null);

    const pagination = reactive<PaginationProps>({
        total: 0,
        pageSize: 10,
        currentPage: 1,
        background: true
    });

    const columns: TableColumnList = [
        {label: "ID", prop: "id", width: 70},
        {label: "文档名", prop: "name", minWidth: 180},
        {
            label: "类型",
            prop: "type",
            width: 100,
            cellRenderer: ({row}) => (
                <el-tag effect="plain">{TYPE_MAP[row.type] || row.type}</el-tag>
            )
        },
        {
            label: "大小",
            width: 90,
            formatter: ({fileSize}) => formatSize(fileSize)
        },
        {
            label: "解析状态",
            prop: "parseStatus",
            width: 110,
            cellRenderer: ({row}) => (
                <el-tag
                    effect="plain"
                    type={STATUS_TYPE[row.parseStatus]}
                    class={row.parseStatus === "PARSING" ? "animate-pulse" : ""}
                >
                    {STATUS_MAP[row.parseStatus] || row.parseStatus}
                </el-tag>
            )
        },
        {label: "上传人", prop: "uploaderName", width: 100},
        {
            label: "上传时间",
            prop: "createTime",
            minWidth: 150,
            formatter: ({createTime}) => dayjs(createTime).format("YYYY-MM-DD HH:mm")
        },
        {label: "操作", fixed: "right", width: 250, slot: "operation"}
    ];

    function formatSize(size: number) {
        if (!size && size !== 0) return "-";
        if (size < 1024) return `${size} B`;
        if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
        return `${(size / 1024 / 1024).toFixed(2)} MB`;
    }

    async function onSearch() {
        loading.value = true;
        try {
            const data: any = await getDocumentList(toRaw(form));
            dataList.value = data?.list || [];
            pagination.total = data?.total || 0;
            pagination.pageSize = data?.pageSize || 10;
            pagination.currentPage = data?.currentPage || 1;
            // PARSING 状态自动轮询（异步解析 UNPARSED→PARSING→PARSED/FAILED）
            const parsing = (data?.list || []).some(
                (d: any) => d.parseStatus === "PARSING"
            );
            if (parsing && !parseTimer.value) {
                parseTimer.value = setInterval(onSearch, 3000);
            } else if (!parsing && parseTimer.value) {
                clearInterval(parseTimer.value);
                parseTimer.value = null;
            }
        } finally {
            loading.value = false;
        }
    }

    const resetForm = el => {
        if (!el) return;
        el.resetFields();
        onSearch();
    };

    function openUpload() {
        addDialog({
            title: "上传文档",
            props: {
                formInline: {
                    projectId: "",
                    type: "OTHER",
                    description: "",
                    file: null,
                    projectOptions: projectOptions.value
                }
            },
            width: "50%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () =>
                h(uploadForm, {ref: uploadFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                uploadFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    if (!cur.file) {
                        message("请选择文档文件", {type: "warning"});
                        return;
                    }
                    await uploadDocument(cur.file, cur.projectId, cur.type, cur.description);
                    message("上传成功，正在异步解析", {type: "success"});
                    done();
                    onSearch();
                });
            }
        });
    }

    function openEdit(row) {
        addDialog({
            title: `编辑文档「${row.name}」`,
            props: {
                formInline: {
                    id: row.id,
                    type: row.type ?? "OTHER",
                    description: row.description ?? ""
                }
            },
            width: "40%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () => h(editForm, {ref: editFormRef, formInline: null}),
            beforeSure: (done, {options}) => {
                const cur = options.props.formInline;
                editFormRef.value.getRef().validate(async (valid: boolean) => {
                    if (!valid) return;
                    await updateDocument(cur.id, cur);
                    message("修改成功", {type: "success"});
                    done();
                    onSearch();
                });
            }
        });
    }

    function openChunks(row) {
        addDialog({
            title: `切片「${row.name}」`,
            props: {
                formInline: {
                    docId: row.id,
                    docName: row.name,
                    parseStatus: row.parseStatus
                }
            },
            width: "60%",
            draggable: true,
            closeOnClickModal: false,
            contentRenderer: () =>
                h(chunksForm, {ref: chunksFormRef, formInline: null}),
            beforeSure: done => {
                done();
            }
        });
    }

    async function handleReparse(row) {
        await reparseDocument(row.id);
        message("已触发重新解析", {type: "success"});
        onSearch();
    }

    function handleDownload(row) {
        downloadDocument(row.id, row.name);
    }

    async function handleDelete(row) {
        await deleteDocument(row.id);
        message("删除成功", {type: "success"});
        onSearch();
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
        const p: any = await getProjectList({pageSize: 100});
        projectOptions.value = (p?.list || []).map((x: any) => ({
            id: x.id,
            name: x.name
        }));
        onSearch();
    });

    onBeforeUnmount(() => {
        if (parseTimer.value) clearInterval(parseTimer.value);
    });

    return {
        form,
        formRef,
        loading,
        columns,
        dataList,
        pagination,
        projectOptions,
        onSearch,
        resetForm,
        openUpload,
        openEdit,
        openChunks,
        handleReparse,
        handleDownload,
        handleDelete,
        handleSizeChange,
        handleCurrentChange
    };
}
