(function() {
    'use strict';

    angular
        .module('jhipsterApp')
        .controller('DepartmentController', DepartmentController);

    DepartmentController.$inject = ['$scope', '$state', 'Department', 'DepartmentSearch'];

    function DepartmentController ($scope, $state, Department, DepartmentSearch) {
        var vm = this;
        
        vm.departments = [];
        vm.search = search;
        vm.loadAll = loadAll;

        loadAll();

        function loadAll() {
            Department.query(function(result) {
                vm.departments = result;
            });
        }

        function search () {
            if (!vm.searchQuery) {
                return vm.loadAll();
            }
            DepartmentSearch.query({query: vm.searchQuery}, function(result) {
                vm.departments = result;
            });
        }    }
})();
