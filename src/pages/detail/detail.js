const app = getApp()
const util = require('../../utils/util.js')

Page({
  data: {
    userinfo: {},
    frontHide: false,
  },
  onLoad: function(options) {
    var that = this
    var userid = options.userid
    
    if (userid != null) {
      //获取收到的卡片的userid
      util.request(wx, '/userinfo/query', {
        userid: userid
      }, function(result) {
        if (result.exist) {
          that.setData({
            userinfo: result.userinfo
          })
        } else {
          util.toast(wx, '该用户不存在', 1250)
        }
      })
    }else{
      var userinfo = wx.getStorageSync('selfPreview')
      console.log(userinfo)
      that.setData({
        userinfo: userinfo
      })
    }
  },
  flipcard: function(e) {
    var that = this
    util.loading(wx, '翻 ~')
    setTimeout(function() {
      that.setData({
        frontHide: !that.data.frontHide,
      })
      wx.hideLoading();
    }, 100)
  },
  //转发
  onShareAppMessage: function () {
    return app.shareAppMessage()
  }
})