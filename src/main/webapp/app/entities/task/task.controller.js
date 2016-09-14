(function() {
    'use strict';

    angular
        .module('jhipsterApp')
        .controller('TaskController', TaskController);

    TaskController.$inject = ['$scope', '$state', 'Task', 'TaskSearch'];

    function TaskController ($scope, $state, Task, TaskSearch) {
        var vm = this;
        
        vm.tasks = [];
        vm.search = search;
        vm.loadAll = loadAll;

        loadAll();

        function loadAll() {
            Task.query(function(result) {
                vm.tasks = result;
            });
        }

        function search () {
            if (!vm.searchQuery) {
                return vm.loadAll();
            }
            TaskSearch.query({query: vm.searchQuery}, function(result) {
                vm.tasks = result;
            });
        }    }
})();
