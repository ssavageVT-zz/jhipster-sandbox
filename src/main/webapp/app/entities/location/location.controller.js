(function() {
    'use strict';

    angular
        .module('jhipsterApp')
        .controller('LocationController', LocationController);

    LocationController.$inject = ['$scope', '$state', 'Location', 'LocationSearch'];

    function LocationController ($scope, $state, Location, LocationSearch) {
        var vm = this;
        
        vm.locations = [];
        vm.search = search;
        vm.loadAll = loadAll;

        loadAll();

        function loadAll() {
            Location.query(function(result) {
                vm.locations = result;
            });
        }

        function search () {
            if (!vm.searchQuery) {
                return vm.loadAll();
            }
            LocationSearch.query({query: vm.searchQuery}, function(result) {
                vm.locations = result;
            });
        }    }
})();
