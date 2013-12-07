angular.module("email", [])
  .factory("auth", function() {
    function Auth (username, password) {
      return true;
    };
    return Auth;
  })
  .controller("email", function($scope, auth) {
    $scope.loggedIn   = false;
    $scope.loginError = false;
    
    $scope.username = "";
    $scope.password = "";
    
    $scope.login = function() {
      if (auth($scope.username, $scope.password)) {
        $scope.loggedIn = true;
        $scope.loginError = false;
      } else {
        $scope.loggedIn = false;
        $scope.loginError = true;
      }
    };
  })
  ;