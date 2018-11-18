// pages/weixin/weixin.js
const app = getApp()
const util = require('../../utils/util.js')

Page({

  data: {
    userid: null,
    friendIds: [],
    objsInfo: []
  },
  onLoad: function () {
    //获取userid
    this.setData({
      userid: app.globalData.userid
    })
    var that = this
    //获取已交换卡片的用户
    util.request(wx, '/cardinfo/getFriends', { userid: this.data.userid }, function (result) {
      that.setData({
        friendIds: result.friendIds
      })
      if (result.friendIds.length != 0) {
        //获取已交换卡片用户的信息
        util.request(wx, '/userinfo/queryAll', {
          params: result.friendIds
        }, function (result) {
          that.setData({
            objsInfo: result
          })
        })
      } else {
        util.toast(wx, '暂无收到的微信号', 1250)
      }
    })
  },
  seeDetail: function (e) {
    var index = e.currentTarget.dataset.index
    wx.navigateTo({
      url: '../detail/detail?userid=' + this.data.objsInfo[index].userid,
    })
  },
  //转发
  onShareAppMessage: function () {
    return app.shareAppMessage()
  }
})