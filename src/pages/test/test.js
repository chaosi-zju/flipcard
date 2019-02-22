// pages/test/test.js
const app = getApp()

Page({
  data: {
    testid:1,
  },
  onLoad: function (options) {
    if (options.testid != null){
      this.setData({
        testid: options.testid
      })
    }
  },
  //转发
  onShareAppMessage: function () {
    return app.shareAppMessage()
  }
})