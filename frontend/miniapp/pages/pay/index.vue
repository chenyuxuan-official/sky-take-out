<!--购买页-->
<template>
  <view class="customer-box">
    <view class="wrap">
      <view class="contion">
        <view class="orderPay">
          <view>
            <view v-if="timeout">订单已超时</view>
            <view v-else
              >支付剩余时间<text>{{ rocallTime }}</text></view
            >
          </view>
          <view class="money"
            >￥<text>{{ orderDataInfo.orderAmount }}</text></view
          >
          <view>{{ shopInfo().shopName }}-{{ orderDataInfo.orderNumber }}</view>
        </view>
      </view>
      <view class="box payBox">
        <view class="contion">
          <view class="example-body">
            <radio-group class="uni-list" @change="styleChange">
              <view class="uni-list-item">
                <view
                  class="uni-list-item__container"
                  v-for="(item, index) in payMethodList"
                  :key="item"
                >
                  <view class="uni-list-item__content">
                    <icon class="wechatIcon"></icon
                    ><text class="uni-list-item__content-title">{{
                      item
                    }}</text>
                  </view>
                  <view class="uni-list-item__extra">
                    <radio
                      :value="item"
                      color="#FFC200"
                      :checked="index == activeRadio"
                      class="radioIcon"
                    />
                  </view>
                </view>
              </view>
            </radio-group>
          </view>
        </view>
      </view>
      <view class="bottomBox btnBox">
        <button
          class="add_btn"
          type="primary"
          plain="true"
          :loading="paying"
          :disabled="paying"
          @click="handleSave"
        >
          确认支付
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState } from "vuex";
import { paymentOrder, cancelOrder } from "@/pages/api/api.js";
export default {
  data() {
    return {
      timeout: false,
      rocallTime: "",
      orderId: null,
      orderDataInfo: {},
      activeRadio: 0,
      payMethodList: ["微信支付"],
      paying: false,
      times: null,
    };
  },
  created() {
    this.orderDataInfo = this.orderData();
  },
  mounted() {
    this.runTimeBack();
  },
  onLoad(options) {
    this.orderId = options.orderId;
  },
  onUnload() {
    clearTimeout(this.times);
  },
  methods: {
    ...mapState(["orderData", "shopInfo"]),
    styleChange(event) {
      const selectedIndex = this.payMethodList.indexOf(event.detail.value);
      if (selectedIndex !== -1) {
        this.activeRadio = selectedIndex;
      }
    },
    // 当前后端采用模拟支付：接口成功时直接更新订单状态，不返回真实微信支付参数。
    async handleSave() {
      if (this.paying) {
        return;
      }

      if (this.timeout) {
        cancelOrder(this.orderId).catch(() => {});
        uni.redirectTo({
          url: "/pages/details/index?orderId=" + this.orderId,
        });
        return;
      }

      const orderNumber = this.orderDataInfo && this.orderDataInfo.orderNumber;
      if (!orderNumber) {
        uni.showToast({ title: "订单信息不完整，请返回重试", icon: "none" });
        return;
      }

      this.paying = true;
      try {
        const res = await paymentOrder({
          orderNumber,
          payMethod: 1,
        });

        // 模拟支付成功时，后端返回的 data 是 null，因此不调用 uni.requestPayment。
        if (!res || (res.code !== 1 && res.code !== 200)) {
          throw new Error((res && res.msg) || "支付请求失败");
        }

        clearTimeout(this.times);
        uni.showToast({ title: "支付成功", icon: "success" });
        setTimeout(() => {
          uni.redirectTo({
            url: "/pages/success/index?orderId=" + this.orderId,
          });
        }, 1500);
      } catch (error) {
        this.paying = false;
        const message =
          (error && error.data && error.data.msg) ||
          (error && error.message) ||
          "支付请求失败，请稍后重试";
        uni.showToast({
          title: message,
          duration: 1500,
          icon: "none",
        });
      }
    },
    // // 订单倒计时
    runTimeBack() {
      const end = Date.parse(this.orderDataInfo.orderTime.replace(/-/g, "/"));
      const now = Date.parse(new Date());
      const m15 = 15 * 60 * 1000;
      const msec = m15 - (now - end);
      if (msec < 0) {
        this.timeout = true;
        clearTimeout(this.times);
      } else {
        let min = parseInt((msec / 1000 / 60) % 60);
        let sec = parseInt((msec / 1000) % 60);
        if (min < 10) {
          min = "0" + min;
        } else {
          min = min;
        }
        if (sec < 10) {
          sec = "0" + sec;
        } else {
          sec = sec;
        }
        this.rocallTime = min + ":" + sec;
        let that = this;
        if (min >= 0 && sec >= 0) {
          if (min === 0 && sec === 0) {
            this.timeout = true;
            clearTimeout(this.times);
            return;
          }
          this.times = setTimeout(function () {
            that.runTimeBack();
          }, 1000);
        }
      }
    },
  },
};
</script>
<style src="./../common/Navbar/navbar.scss" lang="scss" scoped></style>
<style src="./../order/style.scss" lang="scss"></style>
<style>
</style>
