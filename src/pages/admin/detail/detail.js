// pages/personal/personal.js
const app = getApp()
const util = require('../../../utils/util.js')

Page({
  data: {
    userid: null,
    status: null,
    userinfo: {
      //自增userid、默认审核状态status、新增需要wx_openid
      //6个picker :district、gender、groups、education、birthyear、star
      //6个input  :school、hobby、wxnum、phonenum、hobbyImageTitle、hobbyImageDes
      //3个image  :hobbyImage、faceImage、identityImage
    },
    failReason: ''
  },
  //onLoad
  onLoad: function(options) {
    this.setData({
      userid: options.userid,
      status: parseInt(options.status)
    })
    if (this.data.status == 0) {
      wx.setNavigationBarTitle({
        title: '正在审核用户 ' + this.data.userid
      })
    } else if (this.data.status == 1) {
      wx.setNavigationBarTitle({
        title: '已通过用户：' + this.data.userid
      })
    } else {
      wx.setNavigationBarTitle({
        title: '未通过用户：' + this.data.userid
      })
    }

    if (this.data.userid != null) {
      //获取用户信息
      var that = this
      util.request(wx, '/userinfo/query', {
        userid: that.data.userid
      }, function(result) {
        if (result.exist) {
          that.setData({
            userinfo: result.userinfo
          })
        } else {
          util.toast(wx, '用户不存在', 1000)
        }
      })
    } else {
      util.toast(wx, '用户不存在', 1000)
    }
  },
  //审核不通过原因变化
  inputReason: function(e) {
    this.setData({
      failReason: e.detail.value
    })
  },
  //通过审核
  agree: function() {
    var that = this
    wx.showModal({
      title: '通过',
      content: '确认通过该用户的审核吗？',
      success: function(res) {
        if (res.confirm) {
          var user = {
            userid: that.data.userid,
            status: 1,
            failReason: ''
          }
          util.request(wx, '/admin/checkUser', {
            params: user
          }, function(result) {
            that.backToPre()
          })
        }
      }
    })
  },
  //不通过审核
  reject: function() {
    if (this.data.failReason == null || this.data.failReason == '') {
      util.toast(wx, '请输入不通过审核的原因', 1200)
    } else {
      var that = this
      wx.showModal({
        title: '不通过',
        content: '确认不通过该用户的审核吗？',
        success: function(res) {
          if (res.confirm) {
            var user = {
              userid: that.data.userid,
              status: 2,
              failReason: that.data.failReason
            }
            util.request(wx, '/admin/checkUser', {
              params: user
            }, function(result) {
              that.backToPre()
            })
          }
        }
      })
    }
  },
  //删除用户
  delete: function(){
    var userid = this.data.userid
    wx.showModal({
      title: '确认删除',
      content: '你确认要删除/注销该用户吗',
      success: function (res) {
        if (res.confirm) {
          //再次确认
          wx.showModal({
            title: '再次确认删除',
            content: '你真的确认要删除/注销该用户吗？？',
            success: function (res) {
              if (res.confirm) {
                util.request(wx, '/admin/deleteUser', { userid: userid},function(result){
                  util.toast(wx,'删除成功！',1500)
                  var pages = getCurrentPages()
                  var prevPage = pages[pages.length - 2]
                  prevPage.refresh(function () {
                    wx.navigateBack()
                  })
                })
              }
            }
          })
        }
      }
    })
  },
  backToPre: function() {
    var pages = getCurrentPages()
    var prevPage = pages[pages.length - 2]
    prevPage.refreshNotChecked(function() {
      util.toast(wx, '操作成功', 800)
      wx.navigateBack()
    })
  },
  //转发
  onShareAppMessage: function() {
    return app.shareAppMessage()
  }
})