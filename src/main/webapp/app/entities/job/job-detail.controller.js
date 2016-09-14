(function() {
    'use strict';

    angular
        .module('jhipsterApp')
        .controller('JobDetailController', JobDetailController);

    JobDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Job', 'Task', 'Employee'];

    function JobDetailController($scope, $rootScope, $stateParams, previousState, entity, Job, Task, Employee) {
        var vm = this;

        vm.job = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('jhipsterApp:jobUpdate', function(event, result) {
            vm.job = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
