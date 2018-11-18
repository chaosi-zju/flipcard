// pages/signUp/signUp.js
//获取应用实例
const app = getApp()
const util = require('../../utils/util.js')
const FLOWER_COST_EVE = 2

Page({
  data: {
    userid: null, //自己的id
    userstatus: -1, //自己的审核状态
    flowerNum: 0, //玫瑰的数目
    changeNum: 0, //换一批次数
    commendIds: [], //所推荐对象的id的列表
    objsInfo: [], //所推荐对象的信息的列表
    flippedIds: [], //今天已翻开的userids,暂时不需要
    curIndex: 0, //当前对象item的index
    curObj: {}, //当前对象的信息
    curRelation: -1, //和当前对象的关系
    frontHide: false, //是否隐藏卡片正面
    modalHide: true, //是否隐藏模态框
    modalText: '玫瑰不足，可以向好友求助赠送玫瑰，每位好友每天可以赠送一朵玫瑰',
    relationText: ['向Ta发卡片', '已发过卡片', '已交换微信', '已发过卡片']
  },
  onLoad: function () {
    
    this.setData({
      userid: app.globalData.userid,
      userstatus: app.globalData.userstatus,
      commendIds: wx.getStorageSync('commendIds')
    })
    var that = this

    if(this.data.userstatus != -1){
      util.request(wx, '/timerelate/getFlowersAndflippedIds', {
        userid: this.data.userid
      }, function (result) {
        that.setData({
          flowerNum: result.flowerNum,
          changeNum: result.changeNum,
          flippedIds: result.flippedIds
        })
      })
    }
    util.request(wx, '/userinfo/queryAll', {
      params: this.data.commendIds
    }, function (result) {
      that.setData({
        objsInfo: result
      })
    })
  },
  //翻牌子
  flipcard: function () {
    var that = this
    var obj = that.data.objsInfo[that.data.curIndex]

    if(this.data.userid == null){
      //如果是游客
      util.loading(wx, '查看庐山真面目 ~')
      setTimeout(function () {
        that.setData({
          frontHide: true,
          curObj: obj,
          curRelation: -1
        })
        wx.hideLoading()
      }, 500)
    }else{
      //如果已经有userid了
      var ids = {
        userid: that.data.userid,
        objid: obj.userid
      }
      util.request(wx, '/timerelate/flipcard', {
        params: ids
      }, function (result) {
        //如果该卡片已被翻过，则直接进入
        if (!result.firstFlip) {
          util.loading(wx, '已翻 直接查看 ~')
          setTimeout(function () {
            that.setData({
              frontHide: true,
              curObj: obj,
              curRelation: result.curRelation
            })
            wx.hideLoading()
          }, 500)
          //如果没翻过，玫瑰又不够
        } else if (!result.flowerEnough) {
          that.setData({
            modalHide: false
          })
          //如果没翻过，玫瑰够
        } else {
          util.loading(wx, '首次翻 玫瑰 -2')
          //翻开新的要重新计算flowerNum和flippedIds
          that.setData({
            flowerNum: result.flowerNum,
            flippedIds: result.flippedIds,
            curObj: obj,
            curRelation: result.curRelation
          })
          //停止loading
          setTimeout(function () {
            that.setData({
              frontHide: true,
            })
            wx.hideLoading()
          }, 500)
        }
      }, true)
    }
  },
  //收到的微信号
  showRecvWx: function () {
    this.ifHasUserId(function(){
      wx.navigateTo({
        url: '../wxrecv/wxrecv'
      })
    })
  },
  //收到的卡片
  showRecvCard: function () {
    this.ifHasUserId(function(){
      wx.navigateTo({
        url: '../cardrecv/cardrecv',
      })
    })
  },
  backToFront: function () {
    var that = this
    util.loading(wx, '看看下一位 ~')
    setTimeout(function () {
      that.setData({
        frontHide: false
      })
      wx.hideLoading()
    }, 400)
  },
  //发送卡片
  sendCard: function () {
    var that = this
    that.ifHasUserId(function(){
      that.ifHasPassCheck(function () {
        if (that.data.curRelation == -1) {
          var userids = {
            sendId: that.data.userid,
            recvId: that.data.objsInfo[that.data.curIndex].userid
          }
          util.loading(wx, '发送中...')
          util.request(wx, '/cardinfo/sendCard', {
            params: userids
          }, function (result) {
            setTimeout(function () {
              wx.hideLoading();
              that.setData({
                curRelation: 0
              })
              util.toast(wx, '发送成功', 1000)
            }, 500)
          }, true)
        }
      })
    })
  },
  //如果没注册
  ifHasUserId: function(func){
    if (this.data.userid == null){
      wx.showModal({
        title: '未注册',
        content: '您需要完善个人资料并通过审核才能使用该功能',
        showCancel: true,
        cancelText: '再逛逛',
        cancelColor: '#505050',
        confirmText: '完善资料',
        success: function(res){
          if(res.confirm){
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
          }
        }
      })
    }else{
      func()
    }
  },
  //如果没有通过审核
  ifHasPassCheck: function(func){
    if(this.data.userstatus != 1){
      wx.showModal({
        title: '等待审核',
        content: '请等待个人资料通过审核才能使用该功能',
        showCancel: false,
        confirmText: 'OK'
      })
    }else{
      func()
    }
  },
  //点击换一批
  changeBatch: function(){
    var that = this
    that.ifHasUserId(function(){
      if (that.data.changeNum > 0) {
        wx.showModal({
          title: '换一批',
          content: '换一批后，本批卡片将被覆盖，确认现在就换吗？',
          cancelText: '晚点再换',
          cancelColor: '#909090',
          confirmText: '立刻换',
          success: function (res) {
            if (res.confirm) {
              util.request(wx, '/timerelate/changeBatch', { userid: that.data.userid }, function (result) {
                that.setData({
                  commendIds: result.commendIds,
                  changeNum: result.changeNum
                })
                wx.setStorageSync('commendIds', result.commendIds)
                util.request(wx, '/userinfo/queryAll', {
                  params: result.commendIds
                }, function (result) {
                  that.setData({
                    objsInfo: result,
                    flippedIds: [],
                    curIndex: 0
                  })
                })
              })
            }
          }
        })
      } else {
        wx.showModal({
          title: '更换失败',
          content: '抱歉，您的剩余更换次数为0，无法更换，暂且没有获取更换次数的方式，敬请期待',
          showCancel: false
        })
      }
    })
  },
  //swiper被滑动时
  onSwiperChanged: function (e) {
    this.setData({
      curIndex: e.detail.current
    })
  },
  //确定按钮点击事件
  modalConfirm: function () {
    this.setData({
      modalHide: true,
    })
    util.toast(wx,'功能正在开发中...',1200)
  },
  //取消按钮点击事件
  modalCancel: function () {
    this.setData({
      modalHide: true,
    })
  },
  onShareAppMessage: function(){
    return app.shareAppMessage()
  }
})