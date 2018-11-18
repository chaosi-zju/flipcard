const app = getApp()
const util = require('../../utils/util.js')

Page({
  data: {
    userid: null,
    // sendIds: [],
    objsInfo: [],
    frontHide: false,
    curIndex: 0, //当前对象item的index
    curObj: {},  //当前对象的信息
  },
  onLoad: function() {
    //获取userid
    this.setData({
      userid: app.globalData.userid
    })
    var that = this
    //获取收到的卡片的userid
    util.request(wx, '/cardinfo/getReceivedCard', {
      userid: this.data.userid
    }, function(result) {
      // that.setData({
      //   sendIds: result.sendIds
      // })
      if (result.sendIds.length != 0) {
        //获取已交换卡片用户的信息
        util.request(wx, '/userinfo/queryAll', {
          params: result.sendIds
        }, function(result) {
          that.setData({
            objsInfo: result
          })
        })
      } else {
        util.toast(wx, '暂无收到的卡片', 1500)
      }
    })
  },
  deleteCard: function(e) {
    var that = this
    wx.showModal({
      title: '删除卡片',
      content: '您确定删除该卡片吗？',
      success: function(res){
        if(res.confirm){
          that.removeCard('/cardinfo/deleteCard')
        }
      }
    })
  },
  agreeCard: function(e) {
    var that = this
    this.ifHasPassCheck(function(){
      wx.showModal({
        title: '同意卡片',
        content: '您确认同意该卡片吗？同意后将交换微信号',
        success: function (res) {
          if (res.confirm) {
            that.removeCard('/cardinfo/agreeCard')
          }
        }
      })
    }) 
  },
  removeCard: function(url){
    var userids = {
      sendId: this.data.objsInfo[this.data.curIndex].userid,
      recvId: this.data.userid
    }
    var that = this
    util.request(wx, url, { params: userids }, function (result) {
      //删除后的数目和index
      var newNum = that.data.objsInfo.length - 1
      var newIndex = (newNum == 0 ? 0 : that.data.curIndex % newNum)
      //删除卡片  
      // var sendIds = that.data.sendIds
      var objsInfo = that.data.objsInfo
      // sendIds.splice(that.data.curIndex, 1)
      objsInfo.splice(that.data.curIndex, 1)
      //更新
      that.setData({
        // sendIds: sendIds,
        objsInfo: objsInfo,
        frontHide: false,
        curIndex: newIndex,
        curObj: {}
      })
      //如果删除后没有了
      if (newNum <= 0) {
        util.toast(wx, '已经木有剩余的卡片啦 ~', 1000)
        //如果删除后还有
      } else {
        util.toast(wx, 'ok ~', 1000)
      }
    })
  },
  flipcard: function(e) {
    var that = this
    var obj = this.data.objsInfo[this.data.curIndex]
    util.loading(wx, '翻 ~')
    setTimeout(function() {
      that.setData({
        frontHide: !that.data.frontHide,
        curObj: obj,
      })
      wx.hideLoading();
    }, 100)
  },
  //如果没有通过审核
  ifHasPassCheck: function (func) {
    if (app.globalData.userstatus != 1) {
      wx.showModal({
        title: '等待审核',
        content: '请等待个人资料通过审核才能使用该功能',
        showCancel: false,
        confirmText: 'OK'
      })
    } else {
      func()
    }
  },
  onSwiperChanged: function(e) {
    this.setData({
      curIndex: e.detail.current
    })
  },
  //转发
  onShareAppMessage: function () {
    return app.shareAppMessage()
  }
})