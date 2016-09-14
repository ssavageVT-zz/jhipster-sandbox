(function() {
    'use strict';

    angular
        .module('jhipsterApp')
        .controller('RegionController', RegionController);

    RegionController.$inject = ['$scope', '$state', 'Region', 'RegionSearch'];

    function RegionController ($scope, $state, Region, RegionSearch) {
        var vm = this;
        
        vm.regions = [];
        vm.search = search;
        vm.loadAll = loadAll;

        loadAll();

        function loadAll() {
            Region.query(function(result) {
                vm.regions = result;
            });
        }

        function search () {
            if (!vm.searchQuery) {
                return vm.loadAll();
            }
            RegionSearch.query({query: vm.searchQuery}, function(result) {
                vm.regions = result;
            });
        }    }
})();
