const formatTime = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return [year, month, day].map(formatNumber).join('/') + ' ' + [hour, minute, second].map(formatNumber).join(':')
}

const formatNumber = n => {
  n = n.toString()
  return n[1] ? n : '0' + n
}

const AV = require('./av-weapp-min.js')
const BugData = AV.Object.extend('bugdata')

// const host = 'http://192.168.191.2:8080'
// const host = 'http://172.28.218.2:8080'
// const host = 'http://192.168.199.236:8080'
// const host = 'http://localhost:8080'
// const domain = host

const host = 'https://tomcat-sslapi.smoyan.com'
const domain = host+'/flipcard'

const request = function(wx, url, data, func, noloading) {

  if (noloading == null || !noloading){
    wx.showLoading({
      title: 'loading...',
      mask: true
    })
  }
  
  //收集网络错误收据
  var bugData = new BugData()
  bugData.set('url', url)
  bugData.set('data', data)

  wx.request({
    url: domain + url,
    data: data,
    method: 'GET',
    header: {
      'content-type': 'application/json'
    },
    success: function(res) {
      console.log(res.data)
      if (noloading == null || !noloading){
        wx.hideLoading()
      }
      if (res.data.code == '0000') {

        //从服务器获取到相应数据
        var result = res.data.result;
        func(result);

      } else {
        toast(wx, '服务器有问题，数据返回有误', 1000)

        bugData.set('msg', '服务器返回数据问题')
        bugData.set('err', res.data)
        bugData.save().then(function(ress) {
          // console.log(ress)
        }, function(errr) {
          // console.log(errr);
        });
      }
    },
    fail: function(err) {
      console.log('网络错误')
      if (noloading == null || !noloading){
        wx.hideLoading()
      }
      toast(wx, '网络错误，请稍后再试', 1000)

      bugData.set('msg', '网络问题，failed')
      bugData.set('err', err)
      bugData.save().then(function(ress) {
        // console.log(ress)
      }, function(errr) {
        // console.log(errr);
      });
    }
  })
}

const loading = function(wx, title) {
  wx.showLoading({
    title: title,
    mask: true
  })
}

const toast = function(wx, title, time) {
  wx.showToast({
    title: title,
    icon: 'none',
    duration: time
  })
}

module.exports = {
  formatTime: formatTime,
  request: request,
  loading: loading,
  toast: toast,
  host: host,
  domain: domain
}