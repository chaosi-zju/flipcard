const app = getApp()
const util = require('../../utils/util.js')

Page({

  data: {
    users: [],
    userNum: {},
    status: 0
  },
  onLoad: function() {
    var that = this
    this.refreshNotChecked(function() {
      if (that.data.users.length == 0) {
        util.toast(wx, '暂无待审核用户', 1250)
      }
    })
  },
  //查看详细
  seeDetail: function(e) {
    var index = e.currentTarget.dataset.index
    const userid = this.data.users[index].userid
    const status = this.data.status
    wx.navigateTo({
      url: `./detail/detail?userid=${userid}&status=${status}`
    })
  },
  //查看待审核用户
  getNotChecked: function(){
    var that = this
    that.refreshNotChecked(function(){
      wx.setNavigationBarTitle({ title: '待审核用户'})
      if (that.data.users.length == 0) {
        util.toast(wx, '暂无待审核用户', 1250)
      }
    })
  },
  //查看已通过用户
  getChecked: function(){
    var that = this
    that.refreshChecked(function () {
      wx.setNavigationBarTitle({ title: '已通过用户'})
      if (that.data.users.length == 0) {
        util.toast(wx, '暂无已通过用户', 1250)
      }
    })
  },
  //查看未通过用户
  getNotPassed: function () {
    var that = this
    that.refreshNotPassed(function () {
      wx.setNavigationBarTitle({ title: '未通过用户' })
      if (that.data.users.length == 0) {
        util.toast(wx, '暂无未通过用户', 1250)
      }
    })
  },
  //刷新待审核用户
  refreshNotChecked: function(func) {
    var that = this
    //获取待审核用户列表
    util.request(wx, '/admin/getNotCheckUsers', {}, function(result) {
      that.setData({
        users: result.users,
        userNum: result.userNum,
        status: 0
      })
      func()
    })
  },
  //刷新已通过用户
  refreshChecked: function (func) {
    var that = this
    //获取已通过用户列表
    util.request(wx, '/admin/getCheckUsers', {}, function (result) {
      that.setData({
        users: result.users,
        userNum: result.userNum,
        status: 1
      })
      func()
    })
  },
  //刷新未通过用户
  refreshNotPassed: function (func) {
    var that = this
    //获取未通过用户列表
    util.request(wx, '/admin/getNotPassUsers', {}, function (result) {
      that.setData({
        users: result.users,
        userNum: result.userNum,
        status: 2
      })
      func()
    })
  },
  //新增假用户
  addPseudo: function(){
    wx.navigateTo({
      url: '../personal/personal?isAdmin=1'
    })
  },
  refresh: function(func){
    this.refreshNotChecked(func)
  },
  //转发
  onShareAppMessage: function() {
    return app.shareAppMessage()
  }
})