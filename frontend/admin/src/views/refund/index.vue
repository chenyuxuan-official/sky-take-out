<template>
  <div class="dashboard-container">
    <div class="container">
      <div class="tableBar">
        <el-button type="primary" @click="init">刷新</el-button>
        <span class="tip">这里列出用户提交的退款申请，同意后订单将被取消并标记为已退款</span>
      </div>
      <el-table :data="tableData" stripe v-if="tableData.length" class="tableBox">
        <el-table-column prop="number" label="订单号" min-width="180" />
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column prop="orderTime" label="下单时间" min-width="170" />
        <el-table-column label="状态" width="120">
          <template slot-scope="scope">
            <div class="tableColumn-status">退款申请中</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="small"
              class="blueBug"
              @click="handleApprove(scope.row)"
            >
              同意退款
            </el-button>
            <el-button
              type="text"
              size="small"
              class="delBut"
              @click="handleReject(scope.row)"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <Empty v-else />
      <el-pagination
        class="pageList"
        :page-sizes="[10, 20, 30, 40]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="counts"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator'
import Empty from '@/components/Empty/index.vue'
import { getRefundPage, approveRefund, rejectRefund } from '@/api/order'

@Component({
  name: 'Refund',
  components: {
    Empty,
  },
})
export default class extends Vue {
  private counts: number = 0
  private page: number = 1
  private pageSize: number = 10
  private tableData: any[] = []

  created() {
    this.init()
  }

  private async init() {
    const params = {
      page: this.page,
      pageSize: this.pageSize,
    }
    await getRefundPage(params)
      .then((res: any) => {
        if (String(res.data.code) === '1') {
          this.tableData = res.data.data.records || []
          this.counts = res.data.data.total || 0
        }
      })
      .catch(() => {})
  }

  private handleSizeChange(val: any) {
    this.pageSize = val
    this.init()
  }

  private handleCurrentChange(val: any) {
    this.page = val
    this.init()
  }

  private handleApprove(row: any) {
    this.$confirm('同意退款后订单将被取消，确定吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(() => {
        approveRefund({ id: row.id }).then(() => {
          this.$message.success('已同意退款')
          this.init()
        })
      })
      .catch(() => {})
  }

  private handleReject(row: any) {
    this.$confirm('确定拒绝该退款申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(() => {
        rejectRefund({ id: row.id }).then(() => {
          this.$message.success('已拒绝退款申请')
          this.init()
        })
      })
      .catch(() => {})
  }
}
</script>

<style lang="scss" scoped>
.tip {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}
</style>
