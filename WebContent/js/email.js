angular.module("email", [])
  .factory("mail", function($http) {
	/*
	 * This service handles the sending of messages & authentication
	 * Provides checkAuth and send methods which connect to JSON API
	 */
    return {
      checkAuth: function CheckAuth (creds, fail, success) {
    	//add auth header
        var authstring = "Basic " + btoa(creds.user + ":" + creds.pass);
        $http.defaults.headers.common.Authorization = authstring; 

        $http.get("messages")
          .success(success)
          .error(fail);
        },
      send: function Send (creds, message, fail, success) {
    	//add auth header
        var authstring = "Basic " + btoa(creds.user + ":" + creds.pass);
        $http.defaults.headers.common.Authorization = authstring; 

        //send our message in JSON form to our API endpoint
        $http.post("messages", message)
          .success(success)
          .error(fail);
      }
    };
  })
  .factory("modal", function() {
	// Shows the "message sent" dialog
    return function() {
      $('.ui.modal').modal("show");
    };
  })
  .controller("email", function($scope, mail, modal) {
	// main page logic.
	  
	// set initial states:
    $scope.loggedIn   = false;
    $scope.loginError = false;
    $scope.sending    = false;
    $scope.loading    = false;
    
    $scope.username = "";
    $scope.password = "";
    
    $scope.login = function() {
      // run when login form submitted
      function setState(success) {
        return function () {
          $scope.loggedIn   = success;
          $scope.loginError = !success;
          $scope.loading    = false;
        };
      }

      // build callbacks to set form states
      var success = setState(true);
      var fail    = setState(false);

      $scope.loading = true;
      mail.checkAuth({user: $scope.username, pass: $scope.password}, fail, success);
    };
    
    var noop = function () {$scope.sending = false;};
    
    $scope.send = function() {
      var success = function () {
    	  // On success, reset form and show message
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
