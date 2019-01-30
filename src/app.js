//app.js
const AV = require('./utils/av-weapp-min.js')
AV.init({
  appId: 'wpDGiEoTYAvxNYnB11tavIDR-gzGzoHsz',
  appKey: 'RYXgJ8TjUDd3LyHFtP6iSK4P'
});
const util = require('./utils/util.js')

App({
  globalData: {
    wx_info: null,
    userid: null,
    userstatus: null,
    failReason: null,
    matchNum: null
  },
  onLaunch: function() {

    //显示转发分享
    wx.showShareMenu()
    //小程序更新
    this.checkUpdate()
    //获取全局变量wx_info
    this.globalData.wx_info = wx.getStorageSync('wx_info') || {}

    //!wx_info.wx_openid说明第一次玩，进来就分配一个openid
    //!wx_info.nickName说明没有看玩法介绍，即没getUserInfo，即数据库也没记录wx_info
    //只要有了wx_info完整信息，至少说明数据库记录了wx_info，
    //其或许未注册userid为-1，或许注册了有userid，而userstatus=-1/0/1/2(未注册/待审核/通过/不通过)

    //考虑的人：
    //第一次进来,一定能给一个openid，但无wx_info，直到点击了玩法介绍
    //点击过玩法介绍，有wx_info,也有userstatus=-1，但无userid，直到注册
    //注册后，全都有了
  },
  //获取wx_openid
  getOpenId: function() {
    var that = this
    return new Promise(function(resolve) {
      var wx_info = that.globalData.wx_info
      //如果不存在openid就申请,存在就直接进
      if (!wx_info.wx_openid) {
        wx.login({
          success: function(res) {
            if (res.code) {
              util.request(wx, '/index/getOpenId', {
                code: res.code
              }, function(result) {
                // console.log(result)
                wx_info.wx_openid = result.openid;
                wx.setStorageSync('wx_info', wx_info);
                resolve()
              });
            } else {
              util.toast('获取用户登录态失败！' + res.errMsg)
              console.log('获取用户登录态失败！' + res.errMsg)
            }
          }
        });
      } else {
        resolve()
      }
    })
  },
  //获取微信信息
  getWxInfo: function(e) {
    var that = this
    return new Promise(function(resolve) {
      var wx_info = that.globalData.wx_info;
      var userInfo = e.detail.userInfo

      wx_info.wx_nickName = userInfo.nickName
      wx_info.wx_gender = userInfo.gender
      wx_info.wx_province = userInfo.province
      wx_info.wx_city = userInfo.city
      wx_info.wx_avatarUrl = userInfo.avatarUrl
      wx.setStorageSync("wx_info", wx_info)

      resolve()
    })
  },
  //获取用户userid、userstatus、failReason
  getUserId: function() {
    var that = this
    return new Promise(function(resolve) {
      var wx_info = that.globalData.wx_info

      //如果没有openid，补偿措施，不严谨，
      if (!wx_info.wx_openid) {
        that.getOpenId()
          .then(function() {})
        util.toast(wx, '纳尼，我们未获取到您的小程序openid，请再试几次，如果不行请联系管理员MM反馈！', 1000)
      }

      if (wx_info.wx_nickName) {
        util.request(wx, '/userinfo/isUserReg', {
          params: wx_info
        }, function(result) {
          that.globalData.userstatus = result.userstatus
          that.globalData.userid = result.userid
          that.globalData.failReason = result.failReason
          that.globalData.matchNum = result.matchNum
          resolve()
        })
      } else {
        console.log('未看玩法介绍')
        resolve()
      }
    })
  },
  //转发
  shareAppMessage: function() {
    return {
      title: '脉言翻牌子后宫三千 , 送我玫瑰助我一臂之力',
      path: 'pages/rule/introduce/introduce?fromId=' + this.globalData.userid,
      imageUrl: '/image/fan.jpg',
      success: function(res) {
        // 转发成功
        util.toast(wx, '转发成功', 1000)
      },
      fail: function(res) {
        // 转发失败
        util.toast(wx, '转发失败', 1000)
      }
    }
  },
  //检查小程序更新
  checkUpdate: function() {
    // 获取小程序更新机制兼容
    if (wx.canIUse('getUpdateManager')) {
      const updateManager = wx.getUpdateManager()
      updateManager.onCheckForUpdate(function(res) {
        // 请求完新版本信息的回调
        if (res.hasUpdate) {
          updateManager.onUpdateReady(function() {
            wx.showModal({
              title: '更新提示',
              content: '新版本已经准备好，是否重启应用？',
              success: function(res) {
                if (res.confirm) {
                  // 新的版本已经下载好，调用 applyUpdate 应用新版本并重启
                  updateManager.applyUpdate()
                }
              }
            })
          })
          updateManager.onUpdateFailed(function() {
            // 新的版本下载失败
            wx.showModal({
              title: '已经有新版本了哟~',
              content: '新版本已经上线啦~，请您删除当前小程序，重新搜索打开哟~',
            })
          })
        }
      })
    } else {
      // 如果希望用户在最新版本的客户端上体验您的小程序，可以这样子提示
      wx.showModal({
        title: '提示',
        content: '当前微信版本过低，部分功能可能无法使用，请升级到最新微信版本后重试。'
      })
    }
  }
})