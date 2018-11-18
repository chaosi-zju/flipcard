//index.js
//获取应用实例
const app = getApp()
const util = require('../../utils/util.js')
const randomCommendNum = 10;

Page({
  data: {
    userid: null,
    userstatus: null,
    ruleChecked: wx.getStorageSync("ruleChecked"),
    touchStartTime: 0,
    touchEndTime: 0,
    lastTapTime: 0,
    cardFormId: null,
  },
  onLoad: function () {
    this.refresh(function () { })

  },
  //点击玩法介绍，记录用户wx_info，所以如果无wx_info就没看玩法介绍
  getUserInfo: function (e) {
    var that = this
    console.log(e.detail.userInfo)
    //如果拒绝授权
    if (e.detail.userInfo == null) {
      wx.showToast({
        title: '您需要允许微信授权才能继续小程序',
        icon: 'none',
        duration: 3000
      })
      //如果授权
    } else {
      //只会在这getWxInfo
      app.getWxInfo(e)
        .then(app.getUserId)
        .then(function () {
          that.setData({
            userid: app.globalData.userid,
            userstatus: app.globalData.userstatus
          })
          wx.navigateTo({
            url: '../rule/introduce/introduce'
          })
        })
    }
  },
  //点击个人资料
  getPersonal: function () {
    var wx_info = app.globalData.wx_info;
    console.log(wx_info)
    if (!wx_info.wx_nickName) {
      wx.showToast({
        title: '请先阅读玩法介绍',
        icon: 'none',
        duration: 2000
      })
    } else {
      // 进入个人资料
      this.enterPersonal()
    }
  },
  //判断是否可以进入小程序
  canEnterApp: function (e) {
    var that = this
    that.data.cardFormId = e.detail.formId
    app.getUserId().then(function () {
      that.setData({
        userid: app.globalData.userid,
        userstatus: app.globalData.userstatus
      })
      var status = app.globalData.userstatus
      if (status == null) {
        //如果没看玩法介绍
        util.toast(wx, '请先阅读玩法介绍并填写个人资料', 1500)
      } else if (!that.data.ruleChecked) {
        //如果没勾选用户须知
        util.toast(wx, '请先勾选用户须知', 1500)
      } else if (status == -1) {
        that.visitApp(status, '您尚未填写个人资料,请填写资料或先体验游客模式', '填写资料')
      } else if (status == 0) {
        //进入小程序
        that.enterApp()
      } else if (status == 2) {
        that.visitApp(status, '您的资料审核未通过，请更新资料或先体验游客模式', '更新资料')
      } else {
        //进入小程序
        that.enterApp()
      }
    })
  },
  //进入个人资料
  enterPersonal: function () {
    var openid = app.globalData.wx_info.wx_openid
    util.request(wx, '/userinfo/isOpenidExist', {
      openid: openid
    }, function (result) {
      if (result.openidExist) {
        wx.navigateTo({
          url: '../personal/personal'
        })
      } else {
        util.toast(wx, '纳尼！我们没有获取到您的小程序openid，请联系我们的公众号反馈一下！', 2000)
      }
    })
  },
  //进入小程序
  enterApp: function (func) {
    util.request(wx, '/timerelate/getOrUpdateCommend', {
      userid: app.globalData.userid,
      cardFormId: this.data.cardFormId
    }, function (result) {
      var commendIds = result.commendIds
      wx.setStorageSync('commendIds', commendIds)
      wx.navigateTo({
        url: '../main/main'
      })
    })
  },
  //游客访问
  visitApp: function (status, msg, cancelTxt) {
    var that = this
    var wx_info = app.globalData.wx_info
    wx.showModal({
      title: '游客模式',
      content: msg,
      showCancel: true,
      cancelText: cancelTxt,
      cancelColor: '#505050',
      confirmText: '游客模式',
      success: function (res) {
        if (res.confirm) {
          if (status == -1) {
            //如果还未注册
            var commendIds = wx.getStorageSync('commendIds') || []
            if (commendIds.length < randomCommendNum) {
              //如果没给游客分卡片或分的不到10张
              var gender = wx.getStorageSync('gender') || null
              console.log('gender:' + gender)
              if (gender == null) {
                //如果没记录性别，先问性别
                wx.showModal({
                  title: '您的性别',
                  content: '请选择您的性别（不可改）',
                  showCancel: true,
                  cancelText: '男',
                  cancelColor: '#008000',
                  confirmText: '女',
                  cancelColor: '#FF0000',
                  success: function (res) {
                    if (res.confirm) {
                      gender = 2
                    } else {
                      gender = 1
                    }
                    wx.setStorageSync('gender', gender)
                    that.getRandomCommend(commendIds, gender)
                  }
                })
              } else {
                //如果gender存在，不用询问
                that.getRandomCommend(commendIds, gender)
              }
            } else {
              //如果已经分了游客10张卡片
              wx.setStorageSync('commendIds', commendIds)
              wx.navigateTo({
                url: '../main/main'
              })
            }
          } else {
            //如果注册了
            that.enterApp()
          }
        } else {
          // 进入个人资料
          that.enterPersonal()
        }
      }
    })
  },
  getRandomCommend: function (commendIds, gender) {
    var that = this
    //申请随机卡片id
    util.request(wx, '/timerelate/getRandomCommend', {
      commendGender: gender == 1 ? 2 : 1
    }, function (result) {
      var ids = result.commendIds
      for (var i = 0; i < ids.length; i++) {
        if (commendIds.indexOf(ids[i]) == -1) {
          commendIds.push(ids[i])
          if (commendIds.length >= randomCommendNum) break;
        }
      }
      wx.setStorageSync('commendIds', commendIds)
      wx.navigateTo({
        url: '../main/main'
      })
    })
  },
  //勾选用户须知
  boxChanged: function (e) {
    this.data.ruleChecked = !this.data.ruleChecked
    wx.setStorageSync("ruleChecked", this.data.ruleChecked)
  },
  //点击用户须知
  needKnow: function () {
    wx.navigateTo({
      url: '../rule/notice/notice'
    })
  },
  /// 按钮触摸开始触发的事件
  touchStart: function (e) {
    this.touchStartTime = e.timeStamp
  },

  /// 按钮触摸结束触发的事件
  touchEnd: function (e) {
    this.touchEndTime = e.timeStamp
  },
  //双击刷新小程序
  tapRefresh: function (e) {
    var that = this
    // 控制点击事件在350ms内触发，加这层判断是为了防止长按时会触发点击事件
    if (that.touchEndTime - that.touchStartTime < 350) {
      // 当前点击的时间
      var currentTime = e.timeStamp
      var lastTapTime = that.lastTapTime
      // 更新最后一次点击时间
      that.lastTapTime = currentTime

      // 如果两次点击时间在300毫秒内，则认为是双击事件
      if (currentTime - lastTapTime < 300) {
        that.refresh(function () { })
      }
    }
  },
  refresh: function (func) {
    var that = this
    app.getOpenId()
      .then(app.getUserId)
      .then(function () {
        that.setData({
          userid: app.globalData.userid,
          userstatus: app.globalData.userstatus
        })
        func()
      })
  },
  //长按进入管理员
  tapAdmin: function (e) {
    var that = this
    // 控制点击事件超过4秒，进入管理员模式
    var tapTime = that.touchEndTime - that.touchStartTime
    if (tapTime >= 3000 && tapTime <= 9000) {
      var wx_info = app.globalData.wx_info;
      //wx_info信息完整才能申请管理员
      if (wx_info.wx_openid && wx_info.wx_nickName) {
        util.request(wx, '/admin/isAdmin', {
          params: wx_info
        }, function (result) {
          if (result.status == 1) {
            //是管理员，进入管理员界面
            wx.navigateTo({
              url: '../admin/userlist',
            })
          } else {
            console.log('not admin')
          }
        })
      }
    } else if (tapTime > 9000) {
      //长按9秒清空缓存
      wx.showModal({
        title: '提示',
        content: '您确定清空小程序本地缓存?',
        success: function (res) {
          if (res.confirm) {
            wx.clearStorageSync();
            app.globalData.wx_info = {}
            wx.reLaunch({
              url: './index',
            })
          }
        }
      })
    }
  },
  //转发
  onShareAppMessage: function () {
    return app.shareAppMessage()
  }
})