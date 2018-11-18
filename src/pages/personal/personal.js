// pages/personal/personal.js
const app = getApp()
const util = require('../../utils/util.js')

Page({
  data: {
    userid: null,
    userstatus: null,
    failReason: null,
    district: ['杭州', '上海', '成都'], //distIdx
    districtCode: ['330100', '310100', '510100'],
    gender: ['男', '女'], //genderIdx
    groups: ['在校生','上班族'], //groupsIdx
    education: ['本科在读', '硕士在读', '博士在读','学士','硕士','博士'], //eduIdx
    birthyear: [], //yearIdx
    star: ['白羊座', '金牛座', '双子座', '巨蟹座', '狮子座', '处女座', '天秤座', '天蝎座', '射手座', '摩羯座', '水瓶座', '双鱼座'], //starIdx
    userinfo: {
      //自增userid、默认审核状态status、新增需要wx_openid
      //6个picker :district、gender、groups、education、birthyear、star
      //6个input  :school、hobby、wxnum、phonenum、hobbyImageTitle、hobbyImageDes
      //3个image  :hobbyImage、faceImage、identityImage
    },
    isAdmin: false //判断是否是管理员
  },
  //onLoad
  onLoad: function(options) {

    //TODO
    //判断是否是管理员进来
    var isAdmin = options.isAdmin
    this.setData({
      isAdmin: isAdmin != null && isAdmin == '1'
    })

    var years = []
    for (var i = 1985; i <= 2000; i++) {
      years.push(i + '')
    }
    this.setData({
      birthyear: years,
      userid: app.globalData.userid,
      userstatus: app.globalData.userstatus,
      failReason: app.globalData.failReason
    })

    //TODO
    //如果是管理员
    if (this.data.isAdmin == true) {
      this.setData({
        userid: null,
        userstatus: -1,
        failReason: ''
      })
    }

    if (this.data.userid != null) {
      //获取用户信息
      var that = this
      util.request(wx, '/userinfo/query', {
        userid: that.data.userid
      }, function(result) {
        if (result.exist) {
          var userinfo = result.userinfo
          //几个index的转换
          var genderIdx = that.data.gender.indexOf(userinfo.gender)
          var groupsIdx = that.data.groups.indexOf(userinfo.groups)
          var eduIdx = that.data.education.indexOf(userinfo.education)
          var distIdx = that.data.district.indexOf(userinfo.district)
          var yearIdx = that.data.birthyear.indexOf(userinfo.birthyear)
          var starIdx = that.data.star.indexOf(userinfo.star)
          //设置data
          that.setData({
            genderIdx: genderIdx == -1 ? null : genderIdx,
            groupsIdx: groupsIdx == -1 ? null : groupsIdx,
            eduIdx: eduIdx == -1 ? null : eduIdx,
            distIdx: distIdx == -1 ? null : distIdx,
            yearIdx: yearIdx == -1 ? null : yearIdx,
            starIdx: starIdx == -1 ? null : starIdx,
            school: userinfo.school,
            hobby: userinfo.hobby,
            wxnum: userinfo.wxnum,
            phonenum: userinfo.phonenum,
            hobbyImageTitle: userinfo.hobbyImageTitle,
            hobbyImageDes: userinfo.hobbyImageDes,
            hobbyImage: userinfo.hobbyImage,
            faceImage: userinfo.faceImage,
            identityImage: userinfo.identityImage,
          })
        }
      })
    }
  },
  //上传照片，需要裁剪
  upload: function(e) {
    const index = parseInt(e.currentTarget.dataset.index)
    wx.chooseImage({
      count: 1, // 默认9
      sizeType: ['compressed'], // 可以指定是原图还是压缩图，默认二者都有 ['original', 'compressed']
      sourceType: ['album', 'camera'], // 可以指定来源是相册还是相机，默认二者都有
      success(res) {
        const src = res.tempFilePaths[0]
        wx.navigateTo({
          url: `./upload/upload?src=${src}&index=${index}`
        })
      }
    })
  },
  //上传证件照，不需裁剪
  upload2: function() {
    var that = this
    wx.chooseImage({
      count: 1, // 默认9
      sizeType: ['compressed'], // 可以指定是原图还是压缩图，默认二者都有 ['original', 'compressed']
      sourceType: ['album', 'camera'], // 可以指定来源是相册还是相机，默认二者都有
      success(res) {
        const tempFilePath = res.tempFilePaths[0]
        //上传图片
        util.loading(wx, '上传图片中')
        var userid = app.globalData.wx_info.wx_openid
        wx.uploadFile({
          url: util.domain + '/userinfo/uploadImg',
          filePath: tempFilePath,
          name: 'file',
          formData: {
            userid: userid,
            kind: 'identity'
          },
          success: function(res) {
            wx.hideLoading()
            if (res.statusCode == 200) {
              var data = JSON.parse(res.data)
              console.log(data)
              if (data.code == '0000') {
                that.setData({
                  identityImage: util.host + data.result.oriPath
                })
              } else {
                util.toast(wx, '图片上传失败', 1000)
              }
            } else {
              util.toast(wx, '图片上传失败', 1000)
            }
          },
          fail: function(err) {
            wx.hideLoading()
            console.log(err)
            util.toast(wx, '上传中网络出错')
          }
        })
      }
    })
  },
  //预览
  preview: function() {
    var finish = this.checkInfo()
    if (finish && app.globalData.wx_info.wx_openid) {
      var userinfo = this.catUserinfo()
      //其中有几个要改成字符串
      userinfo.district = this.data.district[this.data.distIdx]
      userinfo.gender = this.data.gender[this.data.genderIdx]
      userinfo.groups = this.data.groups[this.data.groupsIdx]
      userinfo.education = this.data.education[this.data.eduIdx]
      console.log(userinfo)
      //暂存本地
      wx.setStorageSync('selfPreview', userinfo)
      wx.navigateTo({
        url: '../detail/detail',
      })
    }
  },
  //提交审核
  submit: function(e) {
    var finish = this.checkInfo()
    //如果信息填写完整且openid存在
    if (finish && app.globalData.wx_info.wx_openid) {
      //TODO
      //如果是管理员在造数据
      if (this.data.isAdmin == true) {
        //提交假数据
        this.addPseudo(e.detail.formId)
      } else {
        var userinfo = this.catUserinfo()
        userinfo.infoFormId = e.detail.formId
        console.log(userinfo)

        util.request(wx, '/userinfo/add', {
          params: userinfo
        }, function(result) {
          app.globalData.userid = result.userid
          app.globalData.userstatus = 0
          // wx.setStorageSync('userid', result.userid)
          // wx.setStorageSync('userstatus', 0)
          wx.showModal({
            title: 'success',
            content: '申请成功，等待审核',
            showCancel: false,
            success: function(res) {
              if (res.confirm) {
                var pages = getCurrentPages()
                var prevPage = pages[pages.length - 2]
                prevPage.refresh(function() {
                  wx.navigateBack()
                })
              }
            }
          })
        })
      }
    }
  },
  //更新资料
  update: function(e) {
    var finish = this.checkInfo()
    //如果信息填写完整
    if (finish) {
      var userinfo = this.catUserinfo()
      userinfo.userid = this.data.userid
      userinfo.infoFormId = e.detail.formId
      console.log(userinfo)

      util.request(wx, '/userinfo/update', {
        params: userinfo
      }, function(result) {
        app.globalData.userstatus = 0
        wx.setStorageSync('userstatus', 0)
        wx.showModal({
          title: 'success',
          content: '更新成功，等待审核',
          showCancel: false,
          success: function(res) {
            if (res.confirm) {
              wx.navigateBack()
            }
          }
        })
      })
    }
  },
  //拼接userinfo
  catUserinfo: function() {
    //先获取wx_openid
    var wx_openid = app.globalData.wx_info.wx_openid

    //拼接userinfo
    var userinfo = {
      wx_openid: wx_openid,
      gender: this.data.genderIdx + 1, // 1男 2女
      groups: this.data.groupsIdx,
      education: this.data.eduIdx,
      district: this.data.districtCode[this.data.distIdx],
      birthyear: this.data.birthyear[this.data.yearIdx],
      star: this.data.star[this.data.starIdx],
      school: this.data.school,
      hobby: this.data.hobby,
      wxnum: this.data.wxnum,
      phonenum: this.data.phonenum,
      hobbyImageTitle: this.data.hobbyImageTitle,
      hobbyImageDes: this.data.hobbyImageDes,
      hobbyImage: this.data.hobbyImage,
      faceImage: this.data.faceImage,
      identityImage: this.data.identityImage,
    }
    return userinfo
  },
  addPseudo: function(infoFormId) {
    var userinfo = this.catUserinfo() //可改
    var wx_info = app.globalData.wx_info //不可改
    var newopenid = 'pseudo_' + new Date().getTime() + '_' + wx_info.wx_openid

    //伪userinfo
    userinfo.wx_openid = newopenid
    userinfo.infoFormId = infoFormId
    //伪wx_info
    var newWx_info = {}
    for(var attr in wx_info){
      newWx_info[attr] = wx_info[attr]
    }
    newWx_info.wx_openid = newopenid
    
    console.log('userinfo:'+JSON.stringify(userinfo))
    console.log('wx_info:' + JSON.stringify(wx_info))

    util.request(wx, '/userinfo/addWxInfo', { params: newWx_info}, function(result){
      util.request(wx, '/userinfo/add',{ params: userinfo }, function (result) {
        wx.showModal({
          title: 'success',
          content: '申请成功，等待审核',
          showCancel: false,
          success: function (res) {
            if (res.confirm) {
              var pages = getCurrentPages()
              var prevPage = pages[pages.length - 2]
              prevPage.refresh(function () {
                wx.navigateBack()
              })
            }
          }
        })
      })
    })

  },
  //检查信息完整
  checkInfo: function() {
    //检查6个picker
    if (this.data.distIdx == null) {
      util.toast(wx, '请选择地域', 1000)
      return false
    }
    if (this.data.genderIdx == null) {
      util.toast(wx, '请选择性别', 1000)
      return false
    }
    if (this.data.yearIdx == null) {
      util.toast(wx, '请选择年龄', 1000)
      return false
    }
    if (this.data.starIdx == null) {
      util.toast(wx, '请选择星座', 1000)
      return false
    }
    if (this.data.groupsIdx == null) {
      util.toast(wx, '请选择类别', 1000)
      return false
    }
    if (this.data.eduIdx == null) {
      util.toast(wx, '请选择学历', 1000)
      return false
    }
    //检查6个input
    if (this.data.school == null || this.data.school == '') {
      util.toast(wx, '请输入学校', 1000)
      return false
    }
    if (this.data.hobby == null || this.data.hobby == '') {
      util.toast(wx, '请输入爱好', 1000)
      return false
    }
    if (this.data.wxnum == null || this.data.wxnum == '') {
      util.toast(wx, '请输入微信号', 1000)
      return false
    }
    if (this.data.phonenum == null || this.data.phonenum == '') {
      util.toast(wx, '请输入手机号', 1000)
      return false
    }
    if (!(/^1[3|4|5|6|7|8|9][0-9]\d{4,8}$/.test(this.data.phonenum))) {
      util.toast(wx, '请输入正确格式的手机号', 1000)
      return false;
    }
    if (this.data.hobbyImageTitle == null || this.data.hobbyImageTitle == '') {
      util.toast(wx, '请输入图片主题', 1000)
      return false
    }
    if (this.data.hobbyImageDes == null || this.data.hobbyImageDes == '') {
      util.toast(wx, '请输入情感表达', 1000)
      return false
    }
    //检查3个图片
    if (this.data.hobbyImage == null) {
      util.toast(wx, '请上传您喜欢的一张图片', 1000)
      return false
    }
    if (this.data.faceImage == null) {
      util.toast(wx, '请上传一张个人照', 1000)
      return false
    }
    if (this.data.identityImage == null) {
      util.toast(wx, '请上传学生证/一卡通图片', 1000)
      return false
    }
    return true
  },
  //更改地域
  districtChange: function(e) {
    this.setData({
      distIdx: parseInt(e.detail.value)
    })
  },
  //更改性别
  genderChange: function(e) {
    this.setData({
      genderIdx: parseInt(e.detail.value)
    })
  },
  //更改年龄
  birthyearChange: function(e) {
    this.setData({
      yearIdx: parseInt(e.detail.value)
    })
  },
  //更改星座
  starChange: function(e) {
    this.setData({
      starIdx: parseInt(e.detail.value)
    })
  },
  //更改类别
  groupsChange: function(e) {
    this.setData({
      groupsIdx: parseInt(e.detail.value)
    })
  },
  //更改学历
  educationChange: function(e) {
    this.setData({
      eduIdx: parseInt(e.detail.value)
    })
  },
  //输入学校
  inputSchool: function(e) {
    this.setData({
      school: e.detail.value
    })
  },
  //输入爱好
  inputHobby: function(e) {
    this.setData({
      hobby: e.detail.value
    })
  },
  //输入微信号
  inputWxnum: function(e) {
    this.setData({
      wxnum: e.detail.value
    })
  },
  //输入手机号
  inputPhone: function(e) {
    this.setData({
      phonenum: e.detail.value
    })
  },
  //输入图片主题
  inputTitle: function(e) {
    this.setData({
      hobbyImageTitle: e.detail.value
    })
  },
  //输入图片描述
  inputDes: function(e) {
    this.setData({
      hobbyImageDes: e.detail.value
    })
  },
  //转发
  onShareAppMessage: function() {
    return app.shareAppMessage()
  }
})