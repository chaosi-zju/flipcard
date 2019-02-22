// pages/rule/introduce/introduce.js
const app = getApp()
const util = require('../../../utils/util.js')

Page({

  data: {
    fromId: null
  },
  onLoad: function(options) {
    var fromId = options.fromId
    if (fromId != null) {
      this.setData({
        fromId: options.fromId
      })
    }
  },
  sendFlower: function(e) {
    var that = this
    this.getUserInfo(e, function() {
      var openid = app.globalData.wx_info.wx_openid
      if (openid) {
        var fromId = that.data.fromId
        if (fromId == 'null' || parseInt(fromId) <= 0) {
          util.toast(wx, '您赠送的用户尚未注册，暂时无法向他赠玫瑰~', 1500)
        } else {
          var ids = {
            openid: openid,
            toid: fromId
          }
          util.request(wx, '/timerelate/giveFlower', {
            params: ids
          }, function(result) {
            //如果所赠送的id存在
            if (result.toidExist) {
              if (result.hasRecord && result.isTodayRecord) {
                util.toast(wx, '您今天已赠送过，每人每天只有一支玫瑰可以赠送噢~', 1250)
              } else {
                util.toast(wx, '赠送成功~', 1000)
              }
            } else {
              util.toast(wx, '您赠送的用户尚未注册，暂时无法向他赠玫瑰~', 1500)
            }
          })
        }
      } else {
        util.toast(wx, 'openid获取失败，赠送不成功', 1000)
      }
    })
  },
  enterApp: function(e) {
    this.getUserInfo(e, function() {
      wx.reLaunch({
        url: '/pages/index/index',
      })
    })
  },
  //已读玩法介绍，记录用户wx_info
  getUserInfo: function(e, func) {
    var that = this
    //如果拒绝授权
    if (e.detail.userInfo == null) {
      wx.showToast({
        title: '您需要允许微信授权才能继续小程序',
        icon: 'none',
        duration: 3000
      })
      //如果授权
    } else {
      app.getWxInfo(e)
        .then(app.getOpenId)
        .then(app.getUserId)
        .then(function() {
          func()
        })
    }
  },
  //转发
  onShareAppMessage: function() {
    return app.shareAppMessage()
  }
})