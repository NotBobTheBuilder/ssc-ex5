angular.module("email", [])
  .factory("mail", function($http) {
    return {
      checkAuth: function CheckAuth (creds, fail, success) {
        var authstring = "Basic " + btoa(creds.user + ":" + creds.pass);
        $http.defaults.headers.common.Authorization = authstring; 

        $http.get("messages")
          .success(success)
          .error(fail);
        },
      send: function Send (creds, message, fail, success) {
        var authstring = "Basic " + btoa(creds.user + ":" + creds.pass);
        $http.defaults.headers.common.Authorization = authstring; 

        $http.post("messages", message)
          .success(success)
          .error(fail);
      }
    };
  })
  .factory("modal", function() {
    return function() {
      $('.ui.modal').modal("show");
    };
  })
  .controller("email", function($scope, mail, modal) {
    $scope.loggedIn   = false;
    $scope.loginError = false;
    $scope.sentModal  = 'hide';
    $scope.sending    = false;
    $scope.loading    = false;
    
    $scope.username = "";
    $scope.password = "";
    
    $scope.login = function() {
      function setState(success) {
        return function () {
          $scope.loggedIn   = success;
          $scope.loginError = !success;
          $scope.loading    = false;
        };
      }

      var success = setState(true);
      var fail    = setState(false);

      $scope.loading = true;
      mail.checkAuth({user: $scope.username, pass: $scope.password}, fail, success);
    };
    
    var noop = function () {$scope.sending = false;};
    
    $scope.send = function() {
      var success = function () {
    	  $scope.to = "";
    	  $scope.subject = "";
    	  $scope.message = "";
    	  $scope.sending = false;
    	  modal();
      };
      $scope.sending = true;

      mail.send({user: $scope.username, pass: $scope.password}, {
        to: $scope.to.split(/,? +/),
        subject: $scope.subject,
        message: $scope.message
      }, noop, success);
    };	
  })
  ;
