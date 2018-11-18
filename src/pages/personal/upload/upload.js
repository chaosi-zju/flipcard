const WeCropper = require('../../../utils/we-cropper.min.js')
const app = getApp()
const util = require('../../../utils/util.js')

const device = wx.getSystemInfoSync()
const width = device.windowWidth
const height = device.windowHeight - 50

Page({
  data: {
    cropperOpt: {
      id: 'cropper',
      width,
      height,
      scale: 2.5,
      zoom: 8,
      cut: {
        x: (width - 300) / 2,
        y: (height - 330) / 2,
        width: 300,
        height: 330
      }
    }
  },
  touchStart(e) {
    this.wecropper.touchStart(e)
  },
  touchMove(e) {
    this.wecropper.touchMove(e)
  },
  touchEnd(e) {
    this.wecropper.touchEnd(e)
  },
  getCropperImage() {
    var that = this
    this.wecropper.getCropperImage((avatar) => {
      if (avatar) {
        var pages = getCurrentPages()
        var prevPage = pages[pages.length - 2]
        var imgName = ['', 'hobby', 'face']
        //上传图片
        util.loading(wx, '上传图片中')
        var userid = app.globalData.wx_info.wx_openid
        wx.uploadFile({
          url: util.domain + '/userinfo/uploadImg',
          filePath: avatar,
          name: 'file',
          formData: {
            userid: userid,
            kind: imgName[that.data.sourceIndex]
          },
          success: function(res) {
            wx.hideLoading()
            // console.log(res)
            if (res.statusCode == 200) {
              var data = JSON.parse(res.data)
              console.log(data)
              if (data.code == '0000') {
                //如果图片获取成功
                if (that.data.sourceIndex == 1) {
                  prevPage.setData({
                    hobbyImage: util.host + data.result.subPath
                  })
                } else {
                  prevPage.setData({
                    faceImage: util.host + data.result.subPath
                  })
                }
                wx.navigateBack()
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
      } else {
        util.toast(wx, '获取图片失败，请稍候再试', 1000)
      }
    })
  },
  uploadTap() {
    const self = this
    wx.chooseImage({
      count: 1, // 默认9
      sizeType: ['original', 'compressed'], // 可以指定是原图还是压缩图，默认二者都有
      sourceType: ['album', 'camera'], // 可以指定来源是相册还是相机，默认二者都有
      success(res) {
        const src = res.tempFilePaths[0]
        //  获取裁剪图片资源后，给data添加src属性及其值
        self.wecropper.pushOrign(src)
      }
    })
  },
  onLoad(option) {
    this.setData({
      sourceIndex: option.index,
      userid: app.globalData.userid
    })
    const {
      cropperOpt
    } = this.data

    if (option.src) {
      cropperOpt.src = option.src
      new WeCropper(cropperOpt)
        .on('ready', (ctx) => {
          // console.log(`wecropper is ready for work!`)
        })
        .on('beforeImageLoad', (ctx) => {
          // console.log(`before picture loaded, i can do something`)
          // console.log(`current canvas context:`, ctx)
          wx.showToast({
            title: '上传中',
            icon: 'loading',
            duration: 20000
          })
        })
        .on('imageLoad', (ctx) => {
          // console.log(`picture loaded`)
          // console.log(`current canvas context:`, ctx)
          wx.hideToast()
        })
        .on('beforeDraw', (ctx, instance) => {
          // console.log(`before canvas draw,i can do something`)
          // console.log(`current canvas context:`, ctx)
        })
        .updateCanvas()
    }
  },
  //转发
  onShareAppMessage: function() {
    return app.shareAppMessage()
  }
})